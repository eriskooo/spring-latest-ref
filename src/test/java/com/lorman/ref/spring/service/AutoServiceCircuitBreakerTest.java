package com.lorman.ref.spring.service;

import com.lorman.ref.spring.client.DummyClient;
import com.lorman.ref.spring.domain.Automobil;
import com.lorman.ref.spring.dto.AutomobilDTO;
import com.lorman.ref.spring.repository.AutoRepository;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class AutoServiceCircuitBreakerTest {

    @Autowired
    private AutoService autoService; // proxy with @CircuitBreaker

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

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

        // Reset circuit breaker state between tests
        circuitBreakerRegistry.circuitBreaker("dummyClient").reset();

        // Execute transaction callbacks directly (no real transaction needed in tests)
        when(transactionTemplate.execute(any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    TransactionCallback<List<AutomobilDTO>> cb = (TransactionCallback<List<AutomobilDTO>>) invocation.getArgument(0);
                    return cb.doInTransaction(Mockito.mock(TransactionStatus.class));
                });

        // Default: repository returns two cars for any pageable (used in fallback path)
        when(repository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(automobil1, automobil2), PageRequest.of(0, 20), 2L));
    }

    @Test
    void circuitBreaker_opens_afterConfiguredFailures_and_fallback_serves_data() {
        // Configure upstream to fail — the fallback will serve DB data instead
        when(dummyClient.callDummy()).thenReturn(Mono.error(new RuntimeException("upstream boom")));

        // Test properties set slidingWindowSize=4, minimumNumberOfCalls=4, failureRateThreshold=50.
        // Each call fails upstream but the fallback returns DB data successfully.
        for (int i = 0; i < 4; i++) {
            StepVerifier.create(autoService.findAll(null, null))
                    .expectNextCount(2)
                    .verifyComplete();
        }

        // After 4 failures the breaker should now be OPEN
        assertThat(circuitBreakerRegistry.circuitBreaker("dummyClient").getState())
                .isEqualTo(CircuitBreaker.State.OPEN);

        // While OPEN, calls are still served by the fallback (no error propagates to the caller)
        StepVerifier.create(autoService.findAll(null, null))
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void circuitBreaker_halfOpen_allows_singleTrial_then_closes_on_success() throws Exception {
        // First: open the breaker with 4 upstream failures (fallback serves data each time)
        when(dummyClient.callDummy()).thenReturn(Mono.error(new RuntimeException("upstream boom")));
        for (int i = 0; i < 4; i++) {
            StepVerifier.create(autoService.findAll(null, null))
                    .expectNextCount(2)
                    .verifyComplete();
        }
        assertThat(circuitBreakerRegistry.circuitBreaker("dummyClient").getState())
                .isEqualTo(CircuitBreaker.State.OPEN);

        // Wait for OPEN waitDuration (1s in test properties) — breaker transitions to HALF_OPEN
        Thread.sleep(1100);

        // Configure a successful upstream path for HALF_OPEN trial
        when(dummyClient.callDummy()).thenReturn(Mono.empty());

        // Trial in HALF_OPEN succeeds — breaker closes
        StepVerifier.create(autoService.findAll(null, null))
                .expectNextCount(2)
                .verifyComplete();

        assertThat(circuitBreakerRegistry.circuitBreaker("dummyClient").getState())
                .isEqualTo(CircuitBreaker.State.CLOSED);
    }
}
