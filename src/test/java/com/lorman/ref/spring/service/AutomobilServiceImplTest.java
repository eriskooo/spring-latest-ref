package com.lorman.ref.spring.service;

import com.lorman.ref.spring.client.DummyClient;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class AutomobilServiceImplTest {

    @Mock
    private AutoRepository repository;
    @Mock
    private DummyClient dummyClient;
    @Mock
    private TransactionTemplate transactionTemplate;

    private AutoServiceImpl service;

    private Automobil automobil1;
    private Automobil automobil2;

    @BeforeEach
    void setUp() {
        AutomobilMapper mapper = Mappers.getMapper(AutomobilMapper.class);
        service = new AutoServiceImpl(repository, mapper, dummyClient, transactionTemplate);
        automobil1 = new Automobil(1L, "Toyota", "Corolla", 2018);
        automobil2 = new Automobil(2L, "VW", "Golf", 2020);
    }

    @Test
    void findAll_withPagination_returnsFirstPageOf10() {
        Mockito.when(dummyClient.callDummy()).thenReturn(Mono.empty());
        Mockito.when(repository.findAll(Mockito.any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(automobil1, automobil2), PageRequest.of(0, 10), 2L));
        Mockito.when(transactionTemplate.execute(Mockito.any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    TransactionCallback<List<AutomobilDTO>> cb = (TransactionCallback<List<AutomobilDTO>>) invocation.getArgument(0);
                    return cb.doInTransaction(Mockito.mock(TransactionStatus.class));
                });

        StepVerifier.create(service.findAll(0, 10))
                .expectNextMatches(a -> a.getId().equals(1L) && a.getBrand().equals("Toyota"))
                .expectNextMatches(a -> a.getId().equals(2L) && a.getBrand().equals("VW"))
                .verifyComplete();
    }

    @Test
    void findAll_withoutPagination_returnsAll() {
        Mockito.when(dummyClient.callDummy()).thenReturn(Mono.empty());
        Mockito.when(repository.findAll())
                .thenReturn(List.of(automobil1, automobil2));
        Mockito.when(transactionTemplate.execute(Mockito.any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    TransactionCallback<List<AutomobilDTO>> cb = (TransactionCallback<List<AutomobilDTO>>) invocation.getArgument(0);
                    return cb.doInTransaction(Mockito.mock(TransactionStatus.class));
                });

        StepVerifier.create(service.findAll(null, null))
                .expectNextMatches(a -> a.getId().equals(1L) && a.getBrand().equals("Toyota"))
                .expectNextMatches(a -> a.getId().equals(2L) && a.getBrand().equals("VW"))
                .verifyComplete();
    }

    @Test
    void findById_returnsMono() {
        Mockito.when(repository.findById(1L)).thenReturn(Optional.of(automobil1));

        StepVerifier.create(service.findById(1L))
                .expectNextMatches(a -> a.getId().equals(1L) && a.getModel().equals("Corolla"))
                .verifyComplete();
    }

    @Test
    void create_setsIdNullAndSaves() {
        AutomobilDTO incoming = new AutomobilDTO(999L, "Skoda", "Octavia", 2019);
        Automobil saved = new Automobil(10L, "Skoda", "Octavia", 2019);
        Mockito.when(repository.save(Mockito.argThat(a -> a.getId() == null))).thenReturn(saved);

        StepVerifier.create(service.create(incoming))
                .expectNextMatches(a -> a.getId().equals(10L) && a.getBrand().equals("Skoda"))
                .verifyComplete();
    }

    @Test
    void update_mergesAndSaves() {
        AutomobilDTO update = new AutomobilDTO(null, "Toyota", "Corolla", 2021);
        Automobil merged = new Automobil(1L, "Toyota", "Corolla", 2021);
        Mockito.when(repository.findById(1L)).thenReturn(Optional.of(automobil1));
        Mockito.when(repository.save(Mockito.any())).thenReturn(merged);

        StepVerifier.create(service.update(1L, update))
                .expectNextMatches(a -> a.getId().equals(1L) && a.getYearMade().equals(2021))
                .verifyComplete();
    }

    @Test
    void deleteById_completes() {
        Mockito.when(repository.findById(1L)).thenReturn(Optional.of(automobil1));
        Mockito.doNothing().when(repository).deleteById(1L);

        StepVerifier.create(service.deleteById(1L))
                .verifyComplete();
    }
}
