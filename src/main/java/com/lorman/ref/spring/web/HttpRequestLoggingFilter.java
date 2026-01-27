package com.lorman.ref.spring.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Reactive HTTP request logging filter for WebFlux.
 * Logs method, path, headers and body (for textual payloads).
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class HttpRequestLoggingFilter implements WebFilter {

    private static final int MAX_LOGGED_BODY_CHARS = 2048; // cap to avoid huge logs
    private static final Set<String> SENSITIVE_HEADERS = Set.of("authorization", "proxy-authorization");

    private static String truncate(String s, int max) {
        if (s == null) return null;
        if (s.length() <= max) return s;
        return s.substring(0, max) + "...<truncated>";
    }

    private static boolean isTextual(MediaType mediaType) {
        if (mediaType == null) return false;
        if (MediaType.APPLICATION_JSON.includes(mediaType)) return true;
        if (MediaType.APPLICATION_XML.includes(mediaType)) return true;
        if (MediaType.TEXT_PLAIN.includes(mediaType)) return true;
        if (MediaType.TEXT_XML.includes(mediaType)) return true;
        if (MediaType.APPLICATION_FORM_URLENCODED.includes(mediaType)) return true;
        // any text/*
        MimeType mt = mediaType;
        return "text".equalsIgnoreCase(mt.getType());
    }

    private static String getCharset(HttpHeaders headers) {
        Charset cs = headers.getContentType() != null && headers.getContentType().getCharset() != null
                ? headers.getContentType().getCharset()
                : StandardCharsets.UTF_8;
        return cs.name();
    }

    private static DataBuffer emptyBuffer(ServerWebExchange exchange) {
        return exchange.getResponse().bufferFactory().wrap(new byte[0]);
    }

    private static byte[] toByteArray(DataBuffer buffer) {
        byte[] bytes = new byte[buffer.readableByteCount()];
        buffer.read(bytes);
        return bytes;
    }

    private static Map<String, List<String>> maskHeaders(HttpHeaders headers) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        headers.forEach((name, values) -> {
            List<String> safeValues = values != null ? values : List.of();
            result.put(name, maskIfSensitive(name, safeValues));
        });
        return result;
    }

    private static List<String> maskIfSensitive(String name, List<String> values) {
        if (!StringUtils.hasText(name)) return values;
        String lower = name.toLowerCase();
        if (SENSITIVE_HEADERS.contains(lower)) {
            return values.stream().map(v -> maskValue(v)).collect(Collectors.toList());
        }
        return values;
    }

    private static String maskValue(String v) {
        if (v == null) return null;
        if (v.length() <= 8) return "********";
        return v.substring(0, 4) + "********" + v.substring(v.length() - 4);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // If body is not present or not loggable, just log without body
        MediaType contentType = request.getHeaders().getContentType();
        boolean logBody = isTextual(contentType);

        if (!logBody) {
            logRequestWithoutBody(request);
            return chain.filter(exchange);
        }

        // Buffer the body so that it can be read for logging and re-exposed downstream
        return DataBufferUtils.join(request.getBody())
                .defaultIfEmpty(emptyBuffer(exchange))
                .flatMap(buffer -> {
                    byte[] bytes = toByteArray(buffer);
                    DataBufferUtils.release(buffer);

                    String charset = getCharset(request.getHeaders());
                    String bodyString = new String(bytes, Charset.forName(charset));
                    String truncated = truncate(bodyString, MAX_LOGGED_BODY_CHARS);

                    logRequestWithBody(request, truncated);

                    // Recreate request with cached body for downstream filters/handlers
                    ServerHttpRequest decorated = new ServerHttpRequestDecorator(request) {
                        @Override
                        public Flux<DataBuffer> getBody() {
                            DataBufferFactory factory = exchange.getResponse().bufferFactory();
                            DataBuffer cached = factory.wrap(bytes);
                            return Flux.defer(() -> Mono.just(DataBufferUtils.retain(cached)));
                        }
                    };

                    return chain.filter(exchange.mutate().request(decorated).build());
                });
    }

    private void logRequestWithoutBody(ServerHttpRequest request) {
        if (log.isInfoEnabled()) {
            log.info("HTTP {} {} headers={}", request.getMethod(), request.getURI().getRawPath(), maskHeaders(request.getHeaders()));
        }
    }

    private void logRequestWithBody(ServerHttpRequest request, String body) {
        if (log.isInfoEnabled()) {
            log.info("HTTP {} {} headers={} body={}", request.getMethod(), request.getURI().getRawPath(), maskHeaders(request.getHeaders()), body);
        }
    }
}
