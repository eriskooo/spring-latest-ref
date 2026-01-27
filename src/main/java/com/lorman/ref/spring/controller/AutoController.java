package com.lorman.ref.spring.controller;

import com.lorman.ref.spring.dto.AutomobilDTO;
import com.lorman.ref.spring.service.AutoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/auta")
@RequiredArgsConstructor
@Validated
public class AutoController {

    private final AutoService service;

    @GetMapping
    public Flux<AutomobilDTO> all() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public Mono<AutomobilDTO> byId(@PathVariable Long id) {
        return service.findById(id);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public Mono<AutomobilDTO> create(@RequestBody @Validated(AutomobilDTO.OnCreate.class) AutomobilDTO item) {
        return service.create(item);
    }

    @PutMapping("/{id}")
    public Mono<AutomobilDTO> update(@PathVariable Long id, @RequestBody @Validated(AutomobilDTO.OnUpdate.class) AutomobilDTO item) {
        return service.update(id, item);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    public Mono<Void> delete(@PathVariable Long id) {
        return service.deleteById(id);
    }
}
