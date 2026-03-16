package com.lorman.ref.spring.service;

import com.lorman.ref.spring.client.DummyClient;
import com.lorman.ref.spring.domain.Automobil;
import com.lorman.ref.spring.dto.AutomobilDTO;
import com.lorman.ref.spring.repository.AutoRepository;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class AutoServiceCircuitBreakerTest {

    @Autowired
    private AutoService autoService; // proxy with @CircuitBreaker

    @MockBean
    private DummyClient dummyClient;

    @MockBean
    private AutoRepository repository;

    @MockBean
    private TransactionTemplate transactionTemplate;

    private Automobil automobil1;
    private Automobil automobil2;

    @BeforeEach
    void initData() {
        automobil1 = new Automobil(1L, "Toyota", "Corolla", 2018);
        automobil2 = new Automobil(2L, "VW", "Golf", 2020);

        // Default transaction template behavior used in success path
        when(transactionTemplate.execute(any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    TransactionCallback<List<AutomobilDTO>> cb = (TransactionCallback<List<AutomobilDTO>>) invocation.getArgument(0);
                    return cb.doInTransaction(Mockito.mock(TransactionStatus.class));
                });
    }

    @Test
    void circuitBreaker_opens_afterConfiguredFailures_and_deniesSubsequentCalls() {
        // Configure upstream to fail
        when(dummyClient.callDummy()).thenReturn(Mono.error(new RuntimeException("upstream boom")));

        // Test properties set slidingWindowSize=4, minimumNumberOfCalls=4, failureRateThreshold=50
        // 4 consecutive failures should OPEN the breaker.
        for (int i = 0; i < 4; i++) {
            StepVerifier.create(autoService.findAll(null, null))
                    .expectError() // underlying error
                    .verify();
        }

        // Next call should be denied immediately by the OPEN breaker
        StepVerifier.create(autoService.findAll(null, null))
                .expectErrorSatisfies(throwable -> {
                    assert throwable instanceof CallNotPermittedException;
                })
                .verify();

        // Note: With Reactor CB aspect, the target method may be invoked to build the Publisher,
        // but the subscription is denied by the breaker. Therefore we do not assert on interactions count here.
    }

    @Test
    void circuitBreaker_halfOpen_allows_singleTrial_then_closes_on_success() throws Exception {
        // First: open the breaker with 4 failures
        when(dummyClient.callDummy()).thenReturn(Mono.error(new RuntimeException("upstream boom")));
        for (int i = 0; i < 4; i++) {
            StepVerifier.create(autoService.findAll(null, null))
                    .expectError()
                    .verify();
        }

        // Wait for OPEN waitDuration (1s in test properties)
        Thread.sleep(1100);

        // Configure a successful path in HALF_OPEN
        when(dummyClient.callDummy()).thenReturn(Mono.empty());
        when(repository.findAll()).thenReturn(List.of(automobil1, automobil2));

        // First trial in HALF_OPEN should succeed and CLOSE the breaker
        StepVerifier.create(autoService.findAll(null, null))
                .expectNextCount(2)
                .verifyComplete();

        // Subsequent call should also pass without being denied
        StepVerifier.create(autoService.findAll(null, null))
                .expectNextCount(2)
                .verifyComplete();
    }
}
