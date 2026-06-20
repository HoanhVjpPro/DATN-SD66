package com.example.datnhathub.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "Product_Detail")
public class ProductDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ProductDetailID")
    private Integer productDetailId;

    @ManyToOne
    @JoinColumn(name = "ProductID", nullable = false)
    private Product product;

    @Column(name = "Size", length = 10)
    private String size;

    @Column(name = "Color", length = 50)
    private String color;

    @Column(name = "Price", precision = 18, scale = 2)
    private BigDecimal price;

    @Column(name = "SKU", length = 50)
    private String sku;
}
