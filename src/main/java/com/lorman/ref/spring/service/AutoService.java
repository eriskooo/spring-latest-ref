package com.lorman.ref.spring.service;

import com.lorman.ref.spring.dto.AutomobilDTO;
import reactor.core.publisher.Mono;

import java.util.List;

public interface AutoService {
    List<AutomobilDTO> findAll();

    Mono<AutomobilDTO> findById(Long id);

    Mono<AutomobilDTO> create(AutomobilDTO item);

    Mono<AutomobilDTO> update(Long id, AutomobilDTO item);

    Mono<Void> deleteById(Long id);
}
