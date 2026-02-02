package com.lorman.ref.spring.client;

import com.lorman.ref.spring.dto.DummyResponseDTO;
import com.lorman.ref.spring.properties.DummyProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class DummyClient {

    private final WebClient.Builder webClientBuilder;
    private final DummyProperties properties;

    public Mono<Void> callDummy() {
        String url = properties.getUrl();
        int port = properties.getPort();

        // normalize base url (remove trailing slash if present)
        String normalizedUrl = url != null && url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        String baseUrl = normalizedUrl + ":" + port;

        // WebClient filters (request/response logging) are applied globally via WebClientCustomizer
        // in OutboundHttpClientLoggingConfig to ensure a single logger for all clients.
        WebClient webClient = webClientBuilder.build();

        return webClient
                .get()
                .uri(baseUrl + "/dummy")
                .retrieve()
                .onStatus(status -> status.value() == 500,
                        resp -> resp.bodyToMono(String.class)
                                .defaultIfEmpty("Upstream returned 500")
                                .flatMap(msg -> {
                                    log.warn("DummyClient: upstream 500 detected, translating to 503. Body={}", msg);
                                    return Mono.error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Translated from 500: " + msg));
                                })
                )
                .bodyToMono(DummyResponseDTO.class)
                .doOnNext(dto -> {
                    if (dto != null) {
                        int number = dto.getNumber();
                        if (number > 5) {
                            log.info("DummyClient: vacsie ako 5 ({}), value={}", number, dto.getValue());
                        } else {
                            log.info("DummyClient: mensie rovne ako 5 ({}), value={}", number, dto.getValue());
                        }
                    } else {
                        log.warn("DummyClient: received null DTO from /dummy");
                    }
                })
                .then();
    }

    /**
     * Test helper (but public): allows forcing a specific number via query param.
     * Preserves the same error translation logic.
     */
    public Mono<Void> callDummyForced(int number) {
        String url = properties.getUrl();
        int port = properties.getPort();

        String normalizedUrl = url != null && url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        String baseUrl = normalizedUrl + ":" + port;

        WebClient webClient = webClientBuilder.build();

        return webClient
                .get()
                .uri(baseUrl + "/dummy?force=" + number)
                .retrieve()
                .onStatus(status -> status.value() == 500,
                        resp -> resp.bodyToMono(String.class)
                                .defaultIfEmpty("Upstream returned 500")
                                .flatMap(msg -> {
                                    log.warn("DummyClient: upstream 500 detected (forced={}), translating to 503. Body={}", number, msg);
                                    return Mono.error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Translated from 500: " + msg));
                                })
                )
                .bodyToMono(DummyResponseDTO.class)
                .then();
    }
}
