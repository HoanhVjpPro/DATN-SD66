package com.example.datnhathub.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "POS_OrderDetail")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PosOrderDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PosOrderDetailID")
    private Integer posOrderDetailId;

    @ManyToOne
    @JoinColumn(name = "PosOrderID", nullable = false)
    private PosOrder posOrder;

    @ManyToOne
    @JoinColumn(name = "ProductDetailID", nullable = false)
    private ProductDetail productDetail;

    @Column(name = "Quantity", nullable = false)
    private Integer quantity;

    @Column(name = "UnitPrice", precision = 18, scale = 2, nullable = false)
    private BigDecimal unitPrice;
}