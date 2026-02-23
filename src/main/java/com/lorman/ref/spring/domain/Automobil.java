package com.lorman.ref.spring.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "AUTOMOBIL")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Automobil {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String brand;

    private String model;

    @Column(name = "YEAR_MADE")
    private Integer yearMade;

    @OneToMany
    @JoinColumn(name = "automobil_id")
    private List<Driver> drivers;

    // Convenience constructor kept for backward compatibility in tests
    public Automobil(Long id, String brand, String model, Integer yearMade) {
        this.id = id;
        this.brand = brand;
        this.model = model;
        this.yearMade = yearMade;
    }
}
