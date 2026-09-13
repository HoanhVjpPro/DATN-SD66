package com.example.datnhathub.dto;

import com.example.datnhathub.entity.ProductDetail;

import java.math.BigDecimal;

public class ProductDetailDto {
    private Integer productDetailId;
    private String color;
    private String size;
    private BigDecimal price;
    private Integer stockQuantity;

    public ProductDetailDto(ProductDetail pd) {
        this.productDetailId = pd.getProductDetailId();
        this.color = pd.getColor();
        this.size = pd.getSize();
        this.price = pd.getPrice();
        this.stockQuantity = pd.getStockQuantity();
    }

    public Integer getProductDetailId() { return productDetailId; }
    public String getColor() { return color; }
    public String getSize() { return size; }
    public BigDecimal getPrice() { return price; }
    public Integer getStockQuantity() { return stockQuantity; }
}