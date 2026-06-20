package com.example.datnhathub.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {
    private Integer productId;
    private String  productName;
    private String  categoryName;
    private String  defaultImage;   // URL ảnh mặc định (IsDefault = 1)
    private BigDecimal minPrice;    // Giá thấp nhất trong các biến thể
    private Boolean status;
}
