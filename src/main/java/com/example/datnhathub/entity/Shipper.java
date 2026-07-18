package com.example.datnhathub.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Shipper")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Shipper {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ShipperID")
    private Integer shipperId;

    @OneToOne
    @JoinColumn(name = "UserID", nullable = false, unique = true)
    private Users user;

    @Column(name = "ShipperCode", length = 20)
    private String shipperCode;
}