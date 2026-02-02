package com.lorman.ref.spring.client;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
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
        StepVerifier.create(dummyClient.callDummy())
                .verifyComplete();
    }
}
