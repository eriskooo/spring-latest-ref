package com.lorman.ref.spring.controller;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AutomobilControllerTest {

    @LocalServerPort
    private int port;

    @Test
    void all_shouldReturnOkAndSomeItems() {
        WebTestClient client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
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
        WebTestClient client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
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

    @Test
    void byId_shouldReturnNestedDriversAndAddressesLoaded() {
        WebTestClient client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();

        client.get()
                .uri("/auta/{id}", 1)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                // drivers collection exists and has at least one element
                .jsonPath("$.drivers").isArray()
                .jsonPath("$.drivers.length()").value(len -> {
                    assertThat((Integer) len).isGreaterThan(0);
                })
                // first driver's fields
                .jsonPath("$.drivers[0].id").exists()
                .jsonPath("$.drivers[0].name").exists()
                .jsonPath("$.drivers[0].surname").exists()
                // addresses collection exists and has at least one element
                .jsonPath("$.drivers[0].addresses").isArray()
                .jsonPath("$.drivers[0].addresses.length()").value(len -> {
                    assertThat((Integer) len).isGreaterThan(0);
                })
                .jsonPath("$.drivers[0].addresses[0].street").exists()
                .jsonPath("$.drivers[0].addresses[0].city").exists();
    }
}
