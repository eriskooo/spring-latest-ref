package com.lorman.ref.spring.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Application-level logging for all WebClient-based HTTP clients.
 * This customizer attaches request/response logging filters to every WebClient.Builder
 * created via Spring Boot autoconfiguration, providing a single, unified logger
 * for outbound HTTP traffic across the application.
 */
@Configuration
@Slf4j
public class OutboundHttpClientLoggingConfig {

    private static ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor((ClientRequest request) -> {
            try {
                log.info("HTTP-OUT Request: {} {}", request.method(), request.url());
                if (!request.headers().isEmpty()) {
                    log.debug("HTTP-OUT Request headers: {}", request.headers());
                }
            } catch (Exception ignored) {
                // best-effort logging
            }
            return Mono.just(request);
        });
    }

    private static ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor((ClientResponse response) -> {
            try {
                log.info("HTTP-OUT Response: status={}", response.statusCode());
                if (!response.headers().asHttpHeaders().isEmpty()) {
                    log.debug("HTTP-OUT Response headers: {}", response.headers().asHttpHeaders());
                }
            } catch (Exception ignored) {
                // best-effort logging
            }
            return Mono.just(response);
        });
    }

    @Bean
    public WebClientCustomizer outboundHttpLoggingCustomizer() {
        return new WebClientCustomizer() {
            @Override
            public void customize(WebClient.Builder builder) {
                builder.filter(logRequest())
                        .filter(logResponse());
            }
        };
    }
}
