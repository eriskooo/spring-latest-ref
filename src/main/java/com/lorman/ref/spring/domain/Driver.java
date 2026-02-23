package com.lorman.ref.spring.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "DRIVER")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Driver {
    @Id
    private Long id;

    @Column(name = "automobil_id", insertable = false, updatable = false)
    private Long automobilId;

    private String name;

    private String surname;

    @OneToMany
    @JoinColumn(name = "driver_id")
    private List<Address> addresses;
}
