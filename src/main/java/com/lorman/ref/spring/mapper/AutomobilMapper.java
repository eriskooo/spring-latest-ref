package com.lorman.ref.spring.mapper;

import com.lorman.ref.spring.domain.Automobil;
import com.lorman.ref.spring.dto.AutomobilDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AutomobilMapper {
    AutomobilDTO toDto(Automobil automobil);

    Automobil toEntity(AutomobilDTO dto);
}
