package com.example.datnhathub.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Entity
@Table(name = "Voucher")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Voucher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "VoucherID")
    private Integer voucherId;

    @Column(name = "Code", length = 50)
    private String code;

    @Column(name = "DiscountAmount", precision = 18, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "Quantity")
    private Integer quantity;

    @Column(name = "ExpiryDate")
    private LocalDate expiryDate;

    // "FIXED" = giảm số tiền cố định | "PERCENT" = giảm theo %
    @Column(name = "DiscountType", length = 20, nullable = false)
    private String discountType = "FIXED";

    @Column(name = "DiscountPercent", precision = 5, scale = 2)
    private BigDecimal discountPercent;

    // Mức giảm tối đa khi dùng loại PERCENT (có thể để trống = không giới hạn)
    @Column(name = "MaxDiscountAmount", precision = 18, scale = 2)
    private BigDecimal maxDiscountAmount;

    // Tính số tiền giảm thực tế dựa trên tổng đơn hàng
    public BigDecimal calculateDiscount(BigDecimal subtotal) {
        if (subtotal == null) subtotal = BigDecimal.ZERO;

        if ("PERCENT".equals(discountType)) {
            if (discountPercent == null) return BigDecimal.ZERO;
            BigDecimal discount = subtotal.multiply(discountPercent)
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            if (maxDiscountAmount != null && discount.compareTo(maxDiscountAmount) > 0) {
                discount = maxDiscountAmount;
            }
            return discount;
        }

        return discountAmount != null ? discountAmount : BigDecimal.ZERO;
    }

    public boolean isFreeShip() {
        return "FREESHIP".equals(discountType);
    }

    public boolean isExpired() {
        return expiryDate != null && expiryDate.isBefore(LocalDate.now());
    }
}
