package com.lorman.ref.spring.controller;

import com.lorman.ref.spring.security.TestJwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AutomobilControllerTest {

    @LocalServerPort
    private int port;

    @Test
    void all_shouldReturnOkAndSomeItems() {
        String token = TestJwtUtil.createToken(java.util.List.of("READ"));
        WebTestClient client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .defaultHeader("Authorization", "Bearer " + token)
                .build();

        client.get()
                .uri("/auta")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$[0].brand").exists();
    }

    @Test
    void byId_shouldReturnSingleEntity() {
        String token = TestJwtUtil.createToken(java.util.List.of("READ"));
        WebTestClient client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .defaultHeader("Authorization", "Bearer " + token)
                .build();

        client.get()
                .uri("/auta/{id}", 1)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.brand").isNotEmpty();
    }
}
