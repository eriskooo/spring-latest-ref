package com.lorman.ref.spring.repository;

import com.lorman.ref.spring.domain.Automobil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
//    @EntityGraph(value = "Automobil.withDriversAndAddresses")
//    @Query("select distinct a from Automobil a left join fetch a.drivers d left join fetch d.addresses")
    List<Automobil> findAll();

    @Override
    @EntityGraph(value = "Automobil.withDrivers")
    Page<Automobil> findAll(Pageable pageable);
}
