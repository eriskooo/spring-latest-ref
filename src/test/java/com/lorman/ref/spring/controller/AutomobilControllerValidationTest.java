package com.lorman.ref.spring.controller;

import com.lorman.ref.spring.security.TestJwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AutomobilControllerValidationTest {

    @LocalServerPort
    private int port;

    private WebTestClient client() {
        // Default client without auth; individual requests will attach proper token
        return WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    @Test
    void create_shouldFailValidation_whenBodyIsInvalid() {
        String token = TestJwtUtil.createToken(java.util.List.of("WRITE"));
        client().post()
                .uri("/auta")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.message").exists();
    }

    @Test
    void update_shouldFailValidation_whenBrandBlank() {
        String payload = "{\n" +
                "  \"brand\": \"\",\n" +
                "  \"model\": \"ModelX\",\n" +
                "  \"yearMade\": 2000\n" +
                "}";

        String token = TestJwtUtil.createToken(java.util.List.of("UPDATE"));
        client().put()
                .uri("/auta/{id}", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.message").exists();
    }
}
