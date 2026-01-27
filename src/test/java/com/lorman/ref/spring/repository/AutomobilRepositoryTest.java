package com.lorman.ref.spring.repository;

import com.lorman.ref.spring.domain.Automobil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@SpringBootTest
class AutomobilRepositoryTest {

    @Autowired
    private AutoRepository repository;

    @Test
    void shouldHaveSeededData() {
        StepVerifier.create(repository.count())
                .expectNext(3L)
                .verifyComplete();

        StepVerifier.create(repository.findAll().take(1))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void crudOperations() {
        Automobil a = new Automobil(null, "Honda", "Civic", 2022);

        Mono<Automobil> flow = repository.save(a)
                .flatMap(saved -> repository.findById(saved.getId()))
                .flatMap(found -> {
                    found.setYearMade(2023);
                    return repository.save(found);
                })
                .flatMap(updated -> repository.deleteById(updated.getId()).thenReturn(updated));

        StepVerifier.create(flow)
                .expectNextMatches(updated -> updated.getYearMade().equals(2023))
                .verifyComplete();
    }
}
