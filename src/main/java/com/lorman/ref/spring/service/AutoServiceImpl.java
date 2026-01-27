package com.lorman.ref.spring.service;

import com.lorman.ref.spring.domain.Automobil;
import com.lorman.ref.spring.dto.AutomobilDTO;
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

    @Override
    public Mono<AutomobilDTO> findById(Long id) {
        return repository.findById(id).map(mapper::toDto);
    }

    @Override
    public Mono<AutomobilDTO> create(AutomobilDTO item) {
        Automobil entity = mapper.toEntity(item);
        entity.setId(null);
        return repository.save(entity).map(mapper::toDto);
    }

    @Override
    public Mono<AutomobilDTO> update(Long id, AutomobilDTO item) {
        return repository.findById(id)
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
        return repository.deleteById(id);
    }
}
