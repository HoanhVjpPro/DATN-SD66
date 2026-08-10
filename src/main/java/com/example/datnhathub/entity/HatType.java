package com.example.datnhathub.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "Hat_Type")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class HatType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "HatTypeID")
    private Integer hatTypeId;

    @Column(name = "HatTypeName", nullable = false, length = 100)
    private String hatTypeName;

    @OneToMany(mappedBy = "hatType")
    private List<Product> products;
}