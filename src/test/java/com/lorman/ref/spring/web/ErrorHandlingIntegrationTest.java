package com.lorman.ref.spring.web;

import com.lorman.ref.spring.dto.AutomobilDTO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ErrorHandlingIntegrationTest {

    @LocalServerPort
    int port;

    private WebTestClient client() {
        return WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    @Test
    void getById_nonExisting_shouldReturn404WithErrorResponse() {
        client().get()
                .uri("/api/autos/{id}", 999999)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.message").value(msg -> ((String) msg).toLowerCase().contains("not found"))
                .jsonPath("$.cause").exists();
    }

    @Test
    void create_withBlankBrand_shouldReturn400() {
        AutomobilDTO invalid = new AutomobilDTO(null, "", "ModelX", 2020);

        client().post()
                .uri("/api/autos")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalid)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.message").value(m -> ((String) m).toLowerCase().contains("brand"))
                .jsonPath("$.cause").exists();
    }

    @Test
    void update_withInvalidYear_shouldReturn400() {
        AutomobilDTO invalid = new AutomobilDTO(null, "Brand", "Model", 1800); // < 1886

        client().put()
                .uri("/api/autos/{id}", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalid)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.message").value(m -> ((String) m).toLowerCase().contains("yearmade"))
                .jsonPath("$.cause").exists();
    }

    @Test
    void delete_nonExisting_shouldReturn404() {
        client().delete()
                .uri("/api/autos/{id}", 888888)
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.message").exists()
                .jsonPath("$.cause").exists();
    }

    @Test
    void getById_withTypeMismatch_shouldReturn400() {
        client().get()
                .uri("/api/autos/{id}", "abc")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.message").exists()
                .jsonPath("$.cause").exists();
    }
}
