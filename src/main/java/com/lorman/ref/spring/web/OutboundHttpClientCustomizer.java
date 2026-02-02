package com.lorman.ref.spring.web;

import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

/**
 * Application-level behavior for all WebClient-based HTTP clients.
 * This customizer attaches a single filter that translates upstream 500 errors
 * to 503 (Service Unavailable) while preserving the original error message.
 */
@Configuration
public class OutboundHttpClientCustomizer {

    /**
     * Globálny prekladač chýb: ak upstream vráti 500, preložíme na 503 (Service Unavailable)
     * a zachováme správu chyby (telo odpovede) v texte výnimky.
     * Toto sa aplikuje na všetky WebClient-ov v aplikácii.
     */
    private static ExchangeFilterFunction translate500To503() {
        return ExchangeFilterFunction.ofResponseProcessor((ClientResponse response) -> {
            if (response.statusCode().value() == 500) {
                return response.bodyToMono(String.class)
                        .defaultIfEmpty("Upstream returned 500")
                        .flatMap(msg -> Mono.error(new ResponseStatusException(
                                HttpStatus.SERVICE_UNAVAILABLE,
                                "Translated from 500: " + msg
                        )));
            }
            return Mono.just(response);
        });
    }

    @Bean
    public WebClientCustomizer outboundHttpLoggingCustomizer() {
        return new WebClientCustomizer() {
            @Override
            public void customize(WebClient.Builder builder) {
                builder.filter(translate500To503());
            }
        };
    }
}
