package com.lorman.ref.spring.mapper;

import com.lorman.ref.spring.domain.Address;
import com.lorman.ref.spring.domain.Automobil;
import com.lorman.ref.spring.domain.Driver;
import com.lorman.ref.spring.dto.AddressDTO;
import com.lorman.ref.spring.dto.AutomobilDTO;
import com.lorman.ref.spring.dto.DriverDTO;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AutomobilMapperTest {

    private final AutomobilMapper mapper = Mappers.getMapper(AutomobilMapper.class);

    @Test
    void toDto_shouldMapAutomobilShallow_withoutDrivers() {
        Automobil auto = new Automobil(1L, "Toyota", "Corolla", 2018);

        AutomobilDTO dto = mapper.toDto(auto);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getBrand()).isEqualTo("Toyota");
        assertThat(dto.getModel()).isEqualTo("Corolla");
        assertThat(dto.getYearMade()).isEqualTo(2018);
        // By design we ignore nested drivers in Automobil mapping
        assertThat(dto.getDrivers()).isNull();
    }

    @Test
    void toDto_shouldMapDriverWithAddresses() {
        Driver driver = new Driver();
        driver.setId(10L);
        driver.setName("John");
        driver.setSurname("Doe");

        Address a1 = new Address(100L, null, "Main Street 1", "Springfield");
        Address a2 = new Address(101L, null, "Second Ave 22", "Springfield");
        driver.setAddresses(List.of(a1, a2));

        DriverDTO dto = mapper.toDto(driver);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(10L);
        assertThat(dto.getName()).isEqualTo("John");
        assertThat(dto.getSurname()).isEqualTo("Doe");
        assertThat(dto.getAddresses()).isNotNull();
        assertThat(dto.getAddresses()).hasSize(2);
        assertThat(dto.getAddresses().get(0).getStreet()).isEqualTo("Main Street 1");
        assertThat(dto.getAddresses().get(1).getCity()).isEqualTo("Springfield");
    }

    @Test
    void toDto_shouldMapAddressSimple() {
        Address address = new Address(200L, null, "Oak Road 5", "Shelbyville");

        AddressDTO dto = mapper.toDto(address);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(200L);
        assertThat(dto.getStreet()).isEqualTo("Oak Road 5");
        assertThat(dto.getCity()).isEqualTo("Shelbyville");
    }

    @Test
    void toEntity_shouldMapDriverDTO_withoutAutomobil_andWithAddresses() {
        AddressDTO a1 = new AddressDTO(1L, "Hlavna 1", "Bratislava");
        AddressDTO a2 = new AddressDTO(2L, "Hradebni 2", "Praha");

        DriverDTO driverDTO = new DriverDTO(5L, "Anna", "Novak", List.of(a1, a2));

        Driver entity = mapper.toEntity(driverDTO);

        assertThat(entity).isNotNull();
        assertThat(entity.getId()).isEqualTo(5L);
        assertThat(entity.getName()).isEqualTo("Anna");
        assertThat(entity.getSurname()).isEqualTo("Novak");
        // automobil relationship is intentionally ignored
        assertThat(entity.getAutomobil()).isNull();
        assertThat(entity.getAddresses()).isNotNull();
        assertThat(entity.getAddresses()).hasSize(2);
        assertThat(entity.getAddresses().get(0).getStreet()).isEqualTo("Hlavna 1");
    }

    @Test
    void toEntity_shouldMapAddressDTO_withoutDriver() {
        AddressDTO dto = new AddressDTO(9L, "Third Blvd 3", "Metro City");

        Address entity = mapper.toEntity(dto);

        assertThat(entity).isNotNull();
        assertThat(entity.getId()).isEqualTo(9L);
        assertThat(entity.getStreet()).isEqualTo("Third Blvd 3");
        assertThat(entity.getCity()).isEqualTo("Metro City");
        // driver relationship is intentionally ignored
        assertThat(entity.getDriver()).isNull();
    }
}
