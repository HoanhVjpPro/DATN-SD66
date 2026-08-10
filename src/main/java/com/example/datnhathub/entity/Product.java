package com.example.datnhathub.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "Product")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ProductID")
    private Integer productId;

    @ManyToOne
    @JoinColumn(name = "CategoryID", nullable = false)
    private Category category;

    @Column(name = "ProductName", nullable = false, length = 255)
    private String productName;

    @Column(name = "Description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "Status")
    private Boolean status = true;

    // Quan hệ 1-n với Product_Detail và Product_Image
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    private List<ProductDetail> details;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    private List<ProductImage> images;

    @ManyToOne
    @JoinColumn(name = "BrandID")
    private Brand brand;

    // Chất liệu (vd: Cotton, Len, Da...)
    @ManyToOne
    @JoinColumn(name = "MaterialID")
    private Material material;

    // Kiểu mũ (vd: Lưỡi trai, Bucket, Beret...)
    @ManyToOne
    @JoinColumn(name = "HatTypeID")
    private HatType hatType;
}