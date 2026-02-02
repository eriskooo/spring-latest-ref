package com.lorman.ref.spring.controller;

import com.lorman.ref.spring.dto.DummyResponseDTO;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.concurrent.ThreadLocalRandom;

@RestController
public class DummyController {

    @GetMapping(value = "/dummy", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<DummyResponseDTO> dummy(@RequestParam(value = "force", required = false) Integer force) {
        int number = force != null ? force : ThreadLocalRandom.current().nextInt(1, 11); // 1..10
        if (number == 5) {
            // vyvoláme 500, GlobalExceptionHandler vráti ErrorResponseDTO
            return Mono.error(new IllegalStateException("Generated forbidden number 5"));
        }
        return Mono.just(new DummyResponseDTO("dummy-ok", number));
    }
}
