package com.lorman.ref.spring.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AutomobilDTO {
    private Long id;

    @NotBlank(groups = {OnCreate.class, OnUpdate.class})
    private String brand;

    @NotBlank(groups = {OnCreate.class, OnUpdate.class})
    private String model;

    @NotNull(groups = {OnCreate.class})
    @Min(value = 1886, groups = {OnCreate.class, OnUpdate.class})
    private Integer yearMade;

    private List<DriverDTO> drivers;

    // Convenience constructor for tests that don't provide nested graph
    public AutomobilDTO(Long id, String brand, String model, Integer yearMade) {
        this.id = id;
        this.brand = brand;
        this.model = model;
        this.yearMade = yearMade;
    }

    /**
     * Validation groups distinguishing rules for create vs update operations.
     */
    public interface OnCreate {
    }

    public interface OnUpdate {
    }
}
