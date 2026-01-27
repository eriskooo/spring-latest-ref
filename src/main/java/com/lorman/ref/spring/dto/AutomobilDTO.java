package com.lorman.ref.spring.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AutomobilDTO {
    private Long id;
    private String brand;
    private String model;
    private Integer yearMade;
}
