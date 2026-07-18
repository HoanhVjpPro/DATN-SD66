package com.example.datnhathub.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "Shipping")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Shipping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ShippingID")
    private Integer shippingId;

    @OneToOne
    @JoinColumn(name = "OrderID", nullable = false, unique = true)
    private Orders order;

    @Column(name = "ShippingAddress", length = 255)
    private String shippingAddress;

    @Column(name = "ShippingStatus", length = 50)
    private String shippingStatus;

    @Column(name = "ShippingFee", precision = 18, scale = 2)
    private BigDecimal shippingFee;

    @ManyToOne
    @JoinColumn(name = "ShipperID")
    private Shipper shipper;

    @Column(name = "ConfirmedByShipper", nullable = false)
    private Boolean confirmedByShipper = false;

    @Column(name = "IncidentReason", length = 500)
    private String incidentReason;
}