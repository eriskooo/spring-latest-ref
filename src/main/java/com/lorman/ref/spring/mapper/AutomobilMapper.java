package com.lorman.ref.spring.mapper;

import com.lorman.ref.spring.domain.Address;
import com.lorman.ref.spring.domain.Automobil;
import com.lorman.ref.spring.domain.Driver;
import com.lorman.ref.spring.dto.AddressDTO;
import com.lorman.ref.spring.dto.AutomobilDTO;
import com.lorman.ref.spring.dto.DriverDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AutomobilMapper {

    AutomobilDTO toDto(Automobil automobil);

    Automobil toEntity(AutomobilDTO dto);

    DriverDTO toDto(Driver driver);

    Driver toEntity(DriverDTO dto);

    AddressDTO toDto(Address address);

    Address toEntity(AddressDTO dto);
}
