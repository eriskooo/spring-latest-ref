package com.lorman.ref.spring.service;

import com.lorman.ref.spring.domain.Automobil;
import com.lorman.ref.spring.dto.AutomobilDTO;
import com.lorman.ref.spring.mapper.AutomobilMapper;
import com.lorman.ref.spring.repository.AutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class AutomobilServiceImplTest {

    @Mock
    private AutoRepository repository;

    private AutoServiceImpl service;

    private Automobil automobil1;
    private Automobil automobil2;

    @BeforeEach
    void setUp() {
        AutomobilMapper mapper = Mappers.getMapper(AutomobilMapper.class);
        service = new AutoServiceImpl(repository, mapper);
        automobil1 = new Automobil(1L, "Toyota", "Corolla", 2018);
        automobil2 = new Automobil(2L, "VW", "Golf", 2020);
    }

    @Test
    void findAll_returnsAll() {
        Mockito.when(repository.findAll()).thenReturn(Flux.just(automobil1, automobil2));

        StepVerifier.create(service.findAll())
                .expectNextMatches(a -> a.getId().equals(1L) && a.getBrand().equals("Toyota"))
                .expectNextMatches(a -> a.getId().equals(2L) && a.getBrand().equals("VW"))
                .verifyComplete();
    }

    @Test
    void findById_returnsMono() {
        Mockito.when(repository.findById(1L)).thenReturn(Mono.just(automobil1));

        StepVerifier.create(service.findById(1L))
                .expectNextMatches(a -> a.getId().equals(1L) && a.getModel().equals("Corolla"))
                .verifyComplete();
    }

    @Test
    void create_setsIdNullAndSaves() {
        AutomobilDTO incoming = new AutomobilDTO(999L, "Skoda", "Octavia", 2019);
        Automobil saved = new Automobil(10L, "Skoda", "Octavia", 2019);
        Mockito.when(repository.save(Mockito.argThat(a -> a.getId() == null))).thenReturn(Mono.just(saved));

        StepVerifier.create(service.create(incoming))
                .expectNextMatches(a -> a.getId().equals(10L) && a.getBrand().equals("Skoda"))
                .verifyComplete();
    }

    @Test
    void update_mergesAndSaves() {
        AutomobilDTO update = new AutomobilDTO(null, "Toyota", "Corolla", 2021);
        Automobil merged = new Automobil(1L, "Toyota", "Corolla", 2021);
        Mockito.when(repository.findById(1L)).thenReturn(Mono.just(automobil1));
        Mockito.when(repository.save(Mockito.any())).thenReturn(Mono.just(merged));

        StepVerifier.create(service.update(1L, update))
                .expectNextMatches(a -> a.getId().equals(1L) && a.getYearMade().equals(2021))
                .verifyComplete();
    }

    @Test
    void deleteById_completes() {
        Mockito.when(repository.deleteById(1L)).thenReturn(Mono.empty());

        StepVerifier.create(service.deleteById(1L))
                .verifyComplete();
    }
}
