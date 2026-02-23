package com.lorman.ref.spring.service;

import com.lorman.ref.spring.client.DummyClient;
import com.lorman.ref.spring.domain.Automobil;
import com.lorman.ref.spring.dto.AutomobilDTO;
import com.lorman.ref.spring.exception.NotFoundException;
import com.lorman.ref.spring.mapper.AutomobilMapper;
import com.lorman.ref.spring.repository.AutoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
public class AutoServiceImpl implements AutoService {

    private final AutoRepository repository;
    private final AutomobilMapper mapper;
    private final DummyClient dummyClient;

    @Override
    public Flux<AutomobilDTO> findAll() {
        // Pred načítaním všetkých áut zavoláme interný /dummy endpoint.
        // Výsledok ani chyby nás nezastavia – po zavolaní pokračujeme ďalej.
        return dummyClient.callDummy()
                .onErrorResume(e -> Mono.empty())
                .thenMany(Flux.defer(() -> Flux.fromIterable(repository.findAll())
                        .map(a -> mapper.toDto(a))))
                .subscribeOn(Schedulers.boundedElastic());
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
