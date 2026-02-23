package com.lorman.ref.spring.mapper;

import com.lorman.ref.spring.domain.Address;
import com.lorman.ref.spring.domain.Automobil;
import com.lorman.ref.spring.domain.Driver;
import com.lorman.ref.spring.dto.AddressDTO;
import com.lorman.ref.spring.dto.AutomobilDTO;
import com.lorman.ref.spring.dto.DriverDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AutomobilMapper {
    AutomobilDTO toDto(Automobil automobil);

    Automobil toEntity(AutomobilDTO dto);

    DriverDTO toDto(Driver driver);

    @Mapping(target = "automobilId", ignore = true)
    Driver toEntity(DriverDTO dto);

    AddressDTO toDto(Address address);

    @Mapping(target = "driverId", ignore = true)
    Address toEntity(AddressDTO dto);
}
