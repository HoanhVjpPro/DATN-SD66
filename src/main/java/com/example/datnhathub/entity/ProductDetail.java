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

    // FK THẬT tới bảng master Product_Size / Product_Color (trước đây lưu String tự do, không ràng buộc)
    @ManyToOne
    @JoinColumn(name = "SizeID")
    private Size sizeEntity;

    @ManyToOne
    @JoinColumn(name = "ColorID")
    private Color colorEntity;

    @Column(name = "Price", precision = 18, scale = 2)
    private BigDecimal price;

    @Column(name = "SKU", length = 50)
    private String sku;

    @Column(name = "StockQuantity", nullable = false)
    private Integer stockQuantity = 0;

    public String getSize() {
        return sizeEntity != null ? sizeEntity.getSizeName() : null;
    }

    public String getColor() {
        return colorEntity != null ? colorEntity.getColorName() : null;
    }
}