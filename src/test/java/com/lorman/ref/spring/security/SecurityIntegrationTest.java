package com.lorman.ref.spring.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecurityIntegrationTest {

    @LocalServerPort
    int port;

    private WebTestClient client() {
        return WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    @Test
    void protectedEndpoint_withoutToken_returns401() {
        client().get()
                .uri("/auta")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void get_withWrongRole_returns403() {
        String token = TestJwtUtil.createToken(List.of("WRITE"));
        client().get()
                .uri("/auta")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void get_withReadRole_returns200() {
        String token = TestJwtUtil.createToken(List.of("READ"));
        client().get()
                .uri("/auta")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk();
    }
}
