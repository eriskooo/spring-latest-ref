package com.lorman.ref.spring.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class DummyController {

    @GetMapping(value = "/dummy", produces = MediaType.TEXT_PLAIN_VALUE)
    public Mono<String> dummy() {
        // Jednoduchý endpoint, ktorý vždy vráti 200 OK
        return Mono.just("dummy-ok");
    }
}
