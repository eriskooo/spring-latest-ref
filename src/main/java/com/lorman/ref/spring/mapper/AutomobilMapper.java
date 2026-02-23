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

    @Mapping(target = "drivers", ignore = true)
    AutomobilDTO toDto(Automobil automobil);

    @Mapping(target = "drivers", ignore = true)
    Automobil toEntity(AutomobilDTO dto);

    DriverDTO toDto(Driver driver);

    @Mapping(target = "automobil", ignore = true)
    Driver toEntity(DriverDTO dto);

    AddressDTO toDto(Address address);

    @Mapping(target = "driver", ignore = true)
    Address toEntity(AddressDTO dto);
}
