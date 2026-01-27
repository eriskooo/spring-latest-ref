package com.lorman.ref.spring.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("AUTOMOBIL")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Automobil {
    @Id
    private Long id;

    private String brand;

    private String model;

    @Column("year_made")
    private Integer yearMade;
}
