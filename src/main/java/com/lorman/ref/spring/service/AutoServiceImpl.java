package com.lorman.ref.spring.service;

import com.lorman.ref.spring.client.DummyClient;
import com.lorman.ref.spring.domain.Automobil;
import com.lorman.ref.spring.dto.AutomobilDTO;
import com.lorman.ref.spring.exception.NotFoundException;
import com.lorman.ref.spring.mapper.AutomobilMapper;
import com.lorman.ref.spring.repository.AutoRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AutoServiceImpl implements AutoService {

    private final AutoRepository repository;
    private final AutomobilMapper mapper;
    private final DummyClient dummyClient;


    private final TransactionTemplate txTemplate;

    @Override
    @CircuitBreaker(name = "dummyClient")
    public Flux<AutomobilDTO> findAll(Integer index, Integer offset) {
        Mono<List<AutomobilDTO>> data = dummyClient.callDummy()
                .then(
                        Mono.fromCallable(() ->
                                txTemplate.execute(status -> {
                                    if (index == null || offset == null) {
                                        return repository.findAll().stream()
                                                .map(mapper::toDto)
                                                .toList();
                                    } else {
                                        Page<Automobil> page = repository.findAll(PageRequest.of(index, offset));
                                        return page.getContent().stream()
                                                .map(mapper::toDto)
                                                .toList();
                                    }
                                })
                        ).subscribeOn(Schedulers.boundedElastic())
                );

        return data.flatMapMany(Flux::fromIterable);
    }

    private static void validate(AutomobilDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Body must not be null");
        }
        if (dto.getBrand() == null || dto.getBrand().isBlank()) {
            throw new IllegalArgumentException("brand must not be blank");
        }
        if (dto.getModel() == null || dto.getModel().isBlank()) {
            throw new IllegalArgumentException("model must not be blank");
        }
        if (dto.getYearMade() != null && dto.getYearMade() < 1886) { // first automobile year
            throw new IllegalArgumentException("yearMade must be >= 1886");
        }
    }

    @Override
    public Mono<AutomobilDTO> findById(Long id) {
        return Mono.defer(() -> Mono.justOrEmpty(repository.findById(id)))
                .switchIfEmpty(Mono.error(new NotFoundException("Automobil not found: " + id)))
                .map(a -> mapper.toDto(a))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<AutomobilDTO> create(AutomobilDTO item) {
        validate(item);
        Automobil entity = mapper.toEntity(item);
        entity.setId(null);
        return Mono.fromCallable(() -> repository.save(entity))
                .map(e -> mapper.toDto(e))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<AutomobilDTO> update(Long id, AutomobilDTO item) {
        validate(item);
        return Mono.fromCallable(() -> repository.findById(id))
                .flatMap(opt -> opt.<Mono<Automobil>>map(Mono::just)
                        .orElseGet(() -> Mono.error(new NotFoundException("Automobil not found: " + id))))
                .map(existing -> {
                    existing.setBrand(item.getBrand());
                    existing.setModel(item.getModel());
                    if (item.getYearMade() != null) {
                        existing.setYearMade(item.getYearMade());
                    }
                    return existing;
                })
                .flatMap(e -> Mono.fromCallable(() -> repository.save(e)))
                .map(saved -> mapper.toDto(saved))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Void> deleteById(Long id) {
        return Mono.fromCallable(() -> repository.findById(id))
                .flatMap(opt -> opt.<Mono<Automobil>>map(Mono::just)
                        .orElseGet(() -> Mono.error(new NotFoundException("Automobil not found: " + id))))
                .flatMap(existing -> Mono.fromRunnable(() -> repository.deleteById(id)))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }
}
