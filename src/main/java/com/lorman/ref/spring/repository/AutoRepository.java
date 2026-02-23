package com.lorman.ref.spring.repository;

import com.lorman.ref.spring.domain.Automobil;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AutoRepository extends JpaRepository<Automobil, Long> {

    @Override
    @EntityGraph(value = "Automobil.withDriversAndAddresses")
    Optional<Automobil> findById(Long id);

    @Override
    @EntityGraph(value = "Automobil.withDrivers")
    List<Automobil> findAll();

}
