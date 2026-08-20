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

@Entity
@Table(name = "POS_Order")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PosOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PosOrderID")
    private Integer posOrderId;

    @ManyToOne
    @JoinColumn(name = "PosCustomerID", nullable = false)
    private PosCustomer posCustomer;

    @ManyToOne
    @JoinColumn(name = "EmployeeID", nullable = false)
    private Employee employee;

    @Column(name = "OrderDate", nullable = false)
    private LocalDateTime orderDate = LocalDateTime.now();

    @Column(name = "TotalAmount", precision = 18, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    // 'CASH' / 'CARD' / 'TRANSFER'
    @Column(name = "PaymentMethod", length = 20, nullable = false)
    private String paymentMethod;

    @Column(name = "Status", length = 20, nullable = false)
    private String status = "Hoàn thành";

    @OneToMany(mappedBy = "posOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PosOrderDetail> details;

    // Mã đơn hiển thị cho khách, VD: POS20260819-15
    public String getOrderCode() {
        String datePart = orderDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "POS" + datePart + "-" + posOrderId;
    }

    public Integer getTotalQuantity() {
        if (details == null) return 0;
        return details.stream().mapToInt(PosOrderDetail::getQuantity).sum();
    }
}