package com.lorman.ref.spring.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.util.List;

@Entity
@Table(name = "AUTOMOBIL")
@Data
@NoArgsConstructor
@AllArgsConstructor
@NamedEntityGraphs({
        @NamedEntityGraph(
                name = "Automobil.withDrivers",
                attributeNodes = {
                        @NamedAttributeNode("drivers")
                }
        ),
        @NamedEntityGraph(
                name = "Automobil.withDriversAndAddresses",
                attributeNodes = {
                        @NamedAttributeNode(value = "drivers", subgraph = "drivers-subgraph")
                },
                subgraphs = {
                        @NamedSubgraph(
                                name = "drivers-subgraph",
                                attributeNodes = {
                                        @NamedAttributeNode("addresses")
                                }
                        )
                }
        )
})
public class Automobil {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String brand;

    private String model;

    @Column(name = "YEAR_MADE")
    private Integer yearMade;

    @OneToMany(
            mappedBy = "automobil",
            cascade = CascadeType.ALL,
            orphanRemoval = false
    )
    @BatchSize(size = 50)
    private List<Driver> drivers;

    // Convenience constructor kept for backward compatibility in tests
    public Automobil(Long id, String brand, String model, Integer yearMade) {
        this.id = id;
        this.brand = brand;
        this.model = model;
        this.yearMade = yearMade;
    }
}
