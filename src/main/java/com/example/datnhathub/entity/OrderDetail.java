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

    // Số lượng khách yêu cầu trả hàng cho riêng dòng sản phẩm này.
    // null hoặc 0 = dòng này không nằm trong yêu cầu trả hàng.
    // Cho phép khách trả một phần đơn hàng (VD: đặt 3 sản phẩm nhưng chỉ 1 sản phẩm bị giao sai)
    // thay vì bắt buộc phải trả toàn bộ đơn.
    @Column(name = "ReturnQuantity")
    private Integer returnQuantity;
}