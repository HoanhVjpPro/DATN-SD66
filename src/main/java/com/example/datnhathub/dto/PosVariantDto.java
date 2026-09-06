package com.example.datnhathub.dto;

import com.example.datnhathub.entity.Product;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PosVariantDto {
    private Integer productDetailId;
    private Product product;
    private String sku;
    private String size;
    private String color;
    private BigDecimal price;
    private Integer stockQuantity;
    private String defaultImageUrl;
}