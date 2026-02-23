package com.lorman.ref.spring.service;

import com.lorman.ref.spring.dto.AutomobilDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AutoService {
    Flux<AutomobilDTO> findAll();

    Mono<AutomobilDTO> findById(Long id);

    Mono<AutomobilDTO> create(AutomobilDTO item);

    Mono<AutomobilDTO> update(Long id, AutomobilDTO item);

    Mono<Void> deleteById(Long id);
}
