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

    // ak chceš mať fyzicky FK stĺpec pod kontrolou cez vzťah:
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "automobil_id")
    private Automobil automobil;

    private String name;

    private String surname;

    @OneToMany(
            mappedBy = "driver",
            cascade = CascadeType.ALL,
            orphanRemoval = false
    )
    private List<Address> addresses;
}
