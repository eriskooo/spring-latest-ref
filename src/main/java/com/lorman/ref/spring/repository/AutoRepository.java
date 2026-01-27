package com.lorman.ref.spring.repository;

import com.lorman.ref.spring.domain.Automobil;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AutoRepository extends ReactiveCrudRepository<Automobil, Long> {
}
