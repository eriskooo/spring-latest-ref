/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2015 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package com.lorman.ref.spring.web;

import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * TraceLogFilter (Spring WebFlux version)
 * <p>
 * This class replaces the original Quarkus/JAX-RS filter with a Spring Boot compatible WebFilter.
 * It logs basic request/response metadata (method, path, headers, status) for all controllers.
 * Outbound WebClient logging is handled separately in OutboundHttpClientLoggingConfig.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class TraceLogFilter implements WebFilter {

    private static final String NOTIFICATION_PREFIX = "* ";
    private static final String REQUEST_PREFIX = "> ";
    private static final String RESPONSE_PREFIX = "< ";
    private static final int MAX_LOGGED_BODY_CHARS = 2048;

    private static boolean isTextual(MediaType mediaType) {
        if (mediaType == null) return false;
        if (MediaType.APPLICATION_JSON.includes(mediaType)) return true;
        if (MediaType.APPLICATION_XML.includes(mediaType)) return true;
        if (MediaType.TEXT_PLAIN.includes(mediaType)) return true;
        if (MediaType.TEXT_XML.includes(mediaType)) return true;
        if (MediaType.APPLICATION_FORM_URLENCODED.includes(mediaType)) return true;
        MimeType mt = mediaType;
        return "text".equalsIgnoreCase(mt.getType());
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        if (s.length() <= max) return s;
        return s.substring(0, max) + "...<truncated>";
    }

    private static String detectCharsetName(HttpHeaders headers) {
        Charset cs = headers != null && headers.getContentType() != null && headers.getContentType().getCharset() != null
                ? headers.getContentType().getCharset()
                : StandardCharsets.UTF_8;
        return cs.name();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String thread = Thread.currentThread().getName();
        long startAt = System.currentTimeMillis();
        String ts = LocalDateTime.now().toString();

        ServerHttpRequest req = exchange.getRequest();
        StringBuilder reqLog = new StringBuilder();
        reqLog.append(ts).append(" [").append(thread).append("] ")
                .append(NOTIFICATION_PREFIX).append("Server has received a request")
                .append(" on thread ").append(thread).append("\n");
        reqLog.append(ts).append(" [").append(thread).append("] ").append("\n")
                .append(REQUEST_PREFIX).append(req.getMethod())
                .append(" ").append(req.getURI().getRawPath()).append("\n");
        printHeaders(reqLog, REQUEST_PREFIX, req.getHeaders());

        boolean logReqBody = isTextual(req.getHeaders().getContentType());

        // Decorate response to capture/log body
        ServerHttpResponse originalResp = exchange.getResponse();
        ServerHttpResponseDecorator respDecorator = new ServerHttpResponseDecorator(originalResp) {
            private boolean metaLogged = false;

            private void logMetaOnce() {
                if (metaLogged) return;
                metaLogged = true;
                logResponse(exchange, thread, startAt);
            }

            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                MediaType ct = getHeaders().getContentType();
                if (isTextual(ct)) {
                    Flux<? extends DataBuffer> flux = Flux.from(body);
                    return DataBufferUtils.join(flux)
                            .flatMap(db -> {
                                byte[] bytes = new byte[db.readableByteCount()];
                                db.read(bytes);
                                DataBufferUtils.release(db);
                                String charset = detectCharsetName(getHeaders());
                                String text = new String(bytes, Charset.forName(charset));
                                String truncated = truncate(text, MAX_LOGGED_BODY_CHARS);
                                logMetaOnce();
                                if (truncated != null && !truncated.isEmpty()) {
                                    log.info(RESPONSE_PREFIX + "body: {}", truncated);
                                }
                                DataBufferFactory f = originalResp.bufferFactory();
                                return super.writeWith(Mono.just(f.wrap(bytes)));
                            })
                            .switchIfEmpty(Mono.defer(() -> {
                                logMetaOnce();
                                return super.writeWith(Flux.empty());
                            }));
                }
                logMetaOnce();
                return super.writeWith(body);
            }

            @Override
            public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
                return writeWith(Flux.from(body).flatMapSequential(p -> p));
            }
        };

        if (!logReqBody) {
            log.info(reqLog.toString());
            return chain.filter(exchange.mutate().response(respDecorator).build());
        }

        // Buffer request body, log and re-expose
        return DataBufferUtils.join(req.getBody())
                .defaultIfEmpty(exchange.getResponse().bufferFactory().wrap(new byte[0]))
                .flatMap(buffer -> {
                    byte[] bytes = new byte[buffer.readableByteCount()];
                    buffer.read(bytes);
                    DataBufferUtils.release(buffer);
                    String charset = detectCharsetName(req.getHeaders());
                    String bodyStr = new String(bytes, Charset.forName(charset));
                    String truncated = truncate(bodyStr, MAX_LOGGED_BODY_CHARS);
                    if (truncated != null && !truncated.isEmpty()) {
                        reqLog.append(REQUEST_PREFIX).append("body: ").append(truncated).append("\n");
                    }
                    log.info(reqLog.toString());

                    ServerHttpRequest decorated = new ServerHttpRequestDecorator(req) {
                        @Override
                        public Flux<DataBuffer> getBody() {
                            DataBufferFactory factory = exchange.getResponse().bufferFactory();
                            DataBuffer cached = factory.wrap(bytes);
                            return Flux.defer(() -> Mono.just(DataBufferUtils.retain(cached)));
                        }
                    };

                    return chain.filter(exchange.mutate().request(decorated).response(respDecorator).build());
                });
    }

    private void logResponse(ServerWebExchange exchange, String thread, long startAt) {
        String ts = LocalDateTime.now().toString();
        long took = System.currentTimeMillis() - startAt;
        StringBuilder b = new StringBuilder();
        int status = exchange.getResponse().getStatusCode() != null ? exchange.getResponse().getStatusCode().value() : 200;
        b.append(ts).append(" [").append(thread).append("] ")
                .append(NOTIFICATION_PREFIX).append("Server responded with a response")
                .append(" on thread ").append(thread).append("\n");
        b.append(ts).append(" [").append(thread).append("] ").append("\n")
                .append(RESPONSE_PREFIX).append(status).append(" (in ").append(took).append(" ms)\n");
        printHeaders(b, RESPONSE_PREFIX, exchange.getResponse().getHeaders());
        log.info(b.toString());
    }

    private void printHeaders(StringBuilder b, String prefix, HttpHeaders headers) {
        if (headers == null || headers.isEmpty()) return;
        for (Map.Entry<String, List<String>> e : headers.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList())) {
            String name = e.getKey();
            List<String> values = e.getValue();
            String value = values == null ? "" : String.join(",", values);
            b.append(prefix).append(name).append(": ").append(maskIfSensitive(name, value)).append("\n");
        }
    }

    private String maskIfSensitive(String name, String value) {
        if (name == null) return value;
        String n = name.toLowerCase();
        if (n.equals("authorization") || n.equals("proxy-authorization")) {
            return maskValue(value);
        }
        return value;
    }

    private String maskValue(String v) {
        if (v == null) return null;
        if (v.length() <= 8) return "********";
        return v.substring(0, 4) + "********" + v.substring(v.length() - 4);
    }
}
