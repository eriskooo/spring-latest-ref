package com.lorman.ref.spring.service;

import com.lorman.ref.spring.client.DummyClient;
import com.lorman.ref.spring.domain.Automobil;
import com.lorman.ref.spring.dto.AutomobilDTO;
import com.lorman.ref.spring.exception.NotFoundException;
import com.lorman.ref.spring.mapper.AutomobilMapper;
import com.lorman.ref.spring.repository.AutoRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutoServiceImpl implements AutoService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;

    private final AutoRepository repository;
    private final AutomobilMapper mapper;
    private final DummyClient dummyClient;
    private final TransactionTemplate txTemplate;

    @Override
    @CircuitBreaker(name = "dummyClient", fallbackMethod = "findAllFallback")
    public Flux<AutomobilDTO> findAll(Integer page, Integer size) {
        Mono<List<AutomobilDTO>> data = dummyClient.callDummy()
                .then(
                        Mono.fromCallable(() ->
                                txTemplate.execute(status -> {
                                    Page<Automobil> p = repository.findAll(
                                            PageRequest.of(page != null ? page : DEFAULT_PAGE,
                                                    size != null ? size : DEFAULT_SIZE));
                                    return p.getContent().stream().map(mapper::toDto).toList();
                                })
                        ).subscribeOn(Schedulers.boundedElastic())
                );

        return data.flatMapMany(Flux::fromIterable);
    }

    // Fallback: circuit is open — skip dummyClient call and serve data directly from DB
    public Flux<AutomobilDTO> findAllFallback(Integer page, Integer size, Throwable t) {
        log.warn("Circuit breaker open for dummyClient, serving data without upstream call: {}", t.getMessage());
        return Mono.fromCallable(() ->
                txTemplate.execute(status -> {
                    Page<Automobil> p = repository.findAll(
                            PageRequest.of(page != null ? page : DEFAULT_PAGE,
                                    size != null ? size : DEFAULT_SIZE));
                    return p.getContent().stream().map(mapper::toDto).toList();
                })
        ).flatMapMany(Flux::fromIterable).subscribeOn(Schedulers.boundedElastic());
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
        Automobil entity = mapper.toEntity(item);
        entity.setId(null);
        return Mono.fromCallable(() -> repository.save(entity))
                .map(e -> mapper.toDto(e))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<AutomobilDTO> update(Long id, AutomobilDTO item) {
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
