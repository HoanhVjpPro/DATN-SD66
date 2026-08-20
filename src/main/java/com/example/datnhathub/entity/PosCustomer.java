package com.example.datnhathub.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "POS_Customer")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PosCustomer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PosCustomerID")
    private Integer posCustomerId;

    @Column(name = "Phone", length = 15, nullable = false, unique = true)
    private String phone;

    @Column(name = "FullName", length = 100, nullable = false)
    private String fullName;

    @Column(name = "Address", length = 255)
    private String address;

    @Column(name = "Email", length = 100)
    private String email;

    @Column(name = "CreatedDate", nullable = false)
    private LocalDateTime createdDate = LocalDateTime.now();
}