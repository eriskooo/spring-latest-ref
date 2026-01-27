package com.lorman.ref.spring.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AutomobilDTO {
    @NotBlank(groups = {OnCreate.class, OnUpdate.class})
    private String brand;
    @NotBlank(groups = {OnCreate.class, OnUpdate.class})
    private String model;

    private Long id;
    @NotNull(groups = {OnCreate.class})
    @Min(value = 1886, groups = {OnCreate.class, OnUpdate.class})
    private Integer yearMade;

    /**
     * Validation groups distinguishing rules for create vs update operations.
     */
    public interface OnCreate {
    }

    public interface OnUpdate {
    }
}
