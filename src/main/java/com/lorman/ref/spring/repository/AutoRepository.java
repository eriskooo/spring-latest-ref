package com.lorman.ref.spring.repository;

import com.lorman.ref.spring.domain.Automobil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AutoRepository extends JpaRepository<Automobil, Long> {
}
