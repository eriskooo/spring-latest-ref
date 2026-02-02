package com.lorman.ref.spring.client;

import com.lorman.ref.spring.dto.DummyResponseDTO;
import com.lorman.ref.spring.properties.DummyProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
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
                .bodyToMono(DummyResponseDTO.class)
                .then();
    }
}
