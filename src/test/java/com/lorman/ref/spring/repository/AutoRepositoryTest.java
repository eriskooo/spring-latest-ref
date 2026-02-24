package com.lorman.ref.spring.repository;

import com.lorman.ref.spring.domain.Address;
import com.lorman.ref.spring.domain.Automobil;
import com.lorman.ref.spring.domain.Driver;
import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "logging.level.org.hibernate.SQL=DEBUG",
        "logging.level.org.hibernate.orm.jdbc.bind=TRACE"
})
class AutoRepositoryTest {

    @Autowired
    AutoRepository repository;

    @Autowired
    EntityManagerFactory emf;

    @Test
    void crudOperations() {
        // Read (seeded by Flyway)
        List<Automobil> all = repository.findAll();
        assertThat(all).isNotEmpty();

        // Create
        Automobil created = new Automobil(null, "Honda", "Civic", 2022);
        Automobil saved = repository.save(created);
        assertThat(saved.getId()).isNotNull();

        // Read by id
        Automobil byId = repository.findById(saved.getId()).orElseThrow();
        assertThat(byId.getBrand()).isEqualTo("Honda");

        // Update
        byId.setModel("Civic e:HEV");
        Automobil updated = repository.save(byId);
        assertThat(updated.getModel()).contains("e:HEV");

        // Delete
        repository.deleteById(updated.getId());
        assertThat(repository.findById(updated.getId())).isEmpty();
    }

    @Test
    void nestedCollections_shouldBeLoaded_andLogQueryCount() {
        // Enable and read Hibernate statistics
        Statistics stats = emf.unwrap(org.hibernate.SessionFactory.class).getStatistics();
        long before = stats.getPrepareStatementCount();

        Automobil auto = repository.findById(1L).orElseThrow();

        // Access lazy collections within the transactional test context
        List<Driver> drivers = auto.getDrivers();
        assertThat(drivers).isNotNull();
        assertThat(drivers).isNotEmpty();

        // Access nested addresses for each driver to trigger loads
        int addressCount = 0;
        for (Driver d : drivers) {
            List<Address> addresses = d.getAddresses();
            assertThat(addresses).isNotNull();
            assertThat(addresses.size()).isGreaterThanOrEqualTo(0);
            addressCount += addresses.size();
        }

        long after = stats.getPrepareStatementCount();
        long delta = after - before;
        System.out.println("[TEST_LOG] SQL prepareStatement count while loading automobil with nested collections = " + delta
                + ", drivers=" + drivers.size() + ", addressesTotal=" + addressCount);

        // Sanity: ensure some addresses exist in the seeded graph
        assertThat(addressCount).isGreaterThan(0);
    }

    @Test
    void findAll_shouldLoadFullGraph_andLogQueryCount() {
        // Enable and read Hibernate statistics
        Statistics stats = emf.unwrap(org.hibernate.SessionFactory.class).getStatistics();
        long before = stats.getPrepareStatementCount();

        List<Automobil> all = repository.findAll();

        long after = stats.getPrepareStatementCount();
        long delta = after - before;

        log.info("*** delta (1) : {}", delta);

        assertThat(all.size()).isGreaterThanOrEqualTo(100);

        Automobil automobil = all.iterator().next();
        assertThat(automobil.getDrivers()).isNotEmpty();

        Driver driver = automobil.getDrivers().iterator().next();
        assertThat(driver.getAddresses()).isNotEmpty();

        after = stats.getPrepareStatementCount();
        delta = after - before;

        log.info("*** delta (2) : {}", delta);
    }

    @Test
    void findAll_withPagination_shouldReturn10PerPage_andDifferentItemsAcrossPages() {
        Page<Automobil> page1 = repository.findAll(PageRequest.of(0, 10));
        Page<Automobil> page2 = repository.findAll(PageRequest.of(1, 10));

        assertThat(page1.getTotalElements()).isGreaterThanOrEqualTo(100);
        assertThat(page1.getContent()).hasSize(10);
        assertThat(page2.getContent()).hasSize(10);

        Set<Long> ids1 = page1.getContent().stream().map(Automobil::getId).collect(Collectors.toSet());
        Set<Long> ids2 = page2.getContent().stream().map(Automobil::getId).collect(Collectors.toSet());

        // Ensure no overlap between first two pages
        assertThat(ids1).doesNotContainAnyElementsOf(ids2);
    }
}
