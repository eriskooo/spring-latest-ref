package com.lorman.ref.spring.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ADDRESS")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Address {
    @Id
    private Long id;

    @Column(name = "driver_id", insertable = false, updatable = false)
    private Long driverId;

    private String street;

    private String city;
}
