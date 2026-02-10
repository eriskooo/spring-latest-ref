package com.lorman.ref.spring.openapi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OpenApiGenerationIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void generateAndStoreOpenApiYamlIntoTarget() throws IOException {
        // when: fetch OpenAPI YAML
        byte[] body = webTestClient
                .get()
                .uri("http://localhost:" + port + "/v3/api-docs.yaml")
                .accept(MediaType.valueOf("application/vnd.oai.openapi"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .returnResult()
                .getResponseBody();

        assertThat(body).isNotNull();
        String yaml = new String(body, StandardCharsets.UTF_8).trim();
        assertThat(yaml).isNotEmpty();

        // then: store into target/openapi.yaml
        Path targetFile = Path.of("target", "openapi.yaml");
        Files.createDirectories(targetFile.getParent());
        Files.writeString(targetFile, yaml, StandardCharsets.UTF_8);

        // and: verify file exists and contains basic OpenAPI markers
        assertThat(Files.exists(targetFile)).isTrue();
        String saved = Files.readString(targetFile, StandardCharsets.UTF_8);
        assertThat(saved).contains("openapi:");
    }
}
