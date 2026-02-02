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
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

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

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String thread = Thread.currentThread().getName();
        long now = System.currentTimeMillis();
        String ts = LocalDateTime.now().toString();

        StringBuilder b = new StringBuilder();
        b.append(ts).append(" [").append(thread).append("] ")
                .append(NOTIFICATION_PREFIX).append("Server has received a request")
                .append(" on thread ").append(thread).append("\n");
        b.append(ts).append(" [").append(thread).append("] ").append("\n")
                .append(REQUEST_PREFIX).append(exchange.getRequest().getMethod())
                .append(" ").append(exchange.getRequest().getURI().getRawPath()).append("\n");

        printHeaders(b, REQUEST_PREFIX, exchange.getRequest().getHeaders());
        log.info(b.toString());

        return chain.filter(exchange)
                .doOnSuccess(v -> logResponse(exchange, thread, now))
                .doOnError(err -> logResponse(exchange, thread, now));
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
