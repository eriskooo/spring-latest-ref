package com.lorman.ref.spring.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverDTO {
    private Long id;
    private String name;
    private String surname;
    private List<AddressDTO> addresses;
}
