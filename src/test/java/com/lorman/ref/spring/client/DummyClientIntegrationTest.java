package com.lorman.ref.spring.client;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;
import reactor.test.StepVerifier;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {
                "server.port=8080",
                "dummy.client.url=http://localhost",
                "dummy.client.port=8080",
                "logging.level.com.lorman.ref.spring.web=INFO"
        }
)
@ActiveProfiles("test")
class DummyClientIntegrationTest {

    @Autowired
    private DummyClient dummyClient;

    @Test
    void clientCallsDummyEndpoint_overHttp_andCompletes() {
        StepVerifier.create(dummyClient.callDummyForced(1))
                .verifyComplete();
    }

    @Test
    void clientTranslates500To503_whenNumberIsFive() {
        StepVerifier.create(dummyClient.callDummyForced(5))
                .expectErrorSatisfies(throwable -> {
                    assert throwable instanceof ResponseStatusException;
                    ResponseStatusException ex = (ResponseStatusException) throwable;
                    assert ex.getStatusCode().value() == HttpStatus.SERVICE_UNAVAILABLE.value();
                })
                .verify();
    }
}
