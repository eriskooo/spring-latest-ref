package com.lorman.ref.spring.service;

import com.lorman.ref.spring.domain.Automobil;
import com.lorman.ref.spring.dto.AutomobilDTO;
import com.lorman.ref.spring.exception.NotFoundException;
import com.lorman.ref.spring.mapper.AutomobilMapper;
import com.lorman.ref.spring.repository.AutoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AutoServiceImpl implements AutoService {

    private final AutoRepository repository;
    private final AutomobilMapper mapper;

    @Override
    public Flux<AutomobilDTO> findAll() {
        return repository.findAll().map(mapper::toDto);
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
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Automobil not found: " + id)))
                .map(mapper::toDto);
    }

    @Override
    public Mono<AutomobilDTO> create(AutomobilDTO item) {
        validate(item);
        Automobil entity = mapper.toEntity(item);
        entity.setId(null);
        return repository.save(entity).map(mapper::toDto);
    }

    @Override
    public Mono<AutomobilDTO> update(Long id, AutomobilDTO item) {
        validate(item);
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Automobil not found: " + id)))
                .flatMap(existing -> {
                    existing.setBrand(item.getBrand());
                    existing.setModel(item.getModel());
                    if (item.getYearMade() != null) {
                        existing.setYearMade(item.getYearMade());
                    }
                    return repository.save(existing);
                })
                .map(mapper::toDto);
    }

    @Override
    public Mono<Void> deleteById(Long id) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Automobil not found: " + id)))
                .flatMap(existing -> repository.deleteById(id));
    }
}
