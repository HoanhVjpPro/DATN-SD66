package com.example.datnhathub.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "Order_Detail")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class OrderDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OrderDetailID")
    private Integer orderDetailId;

    @ManyToOne
    @JoinColumn(name = "OrderID", nullable = false)
    private Orders order;

    @ManyToOne
    @JoinColumn(name = "ProductDetailID", nullable = false)
    private ProductDetail productDetail;

    @Column(name = "Quantity")
    private Integer quantity;

    @Column(name = "UnitPrice", precision = 18, scale = 2)
    private BigDecimal unitPrice;
}
