package com.example.datnhathub.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "Orders")
public class Orders {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OrderID")
    private Integer orderId;

    @ManyToOne
    @JoinColumn(name = "CustomerID", nullable = false)
    private Customer customer;

    // Có thể null lúc đặt hàng (chưa gán nhân viên xử lý)
    @ManyToOne
    @JoinColumn(name = "EmployeeID")
    private Employee employee;

    @Column(name = "TotalAmount", precision = 18, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "OrderDate")
    private LocalDateTime orderDate;

    @Column(name = "Status", length = 50)
    private String status;

    @Column(name = "ReturnStatus", length = 50)
    private String returnStatus;

    @Column(name = "ReturnReason", length = 500)
    private String returnReason;

    @Column(name = "ReturnDate")
    private LocalDateTime returnDate;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderDetail> details;

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL)
    private Shipping shipping;

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL)
    private Payment payment;

    @ManyToOne
    @JoinColumn(name = "VoucherID")
    private Voucher voucher;

    public String getOrderCode() {
        String datePart = orderDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "HH" + datePart + orderId;
    }

    @Column(name = "StockDeducted", nullable = false)
    private Boolean stockDeducted = false;

    public Integer getTotalQuantity() {
        if (details == null) return 0;
        return details.stream()
                .mapToInt(OrderDetail::getQuantity)
                .sum();
    }

    public BigDecimal getSubtotal() {
        if (details == null) return BigDecimal.ZERO;
        return details.stream()
                .map(d -> d.getUnitPrice().multiply(BigDecimal.valueOf(d.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
