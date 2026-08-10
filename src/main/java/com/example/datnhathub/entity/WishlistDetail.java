package com.example.datnhathub.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "Wishlist_Detail", uniqueConstraints = @UniqueConstraint(
        name = "UQ_Wishlist_Product",
        columnNames = {"WishlistID", "ProductID"}
))
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class WishlistDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "WishlistDetailID")
    private Integer wishlistDetailId;

    @ManyToOne
    @JoinColumn(name = "WishlistID", nullable = false)
    private Wishlist wishlist;

    // Lưu theo Product (không phải ProductDetail) — khách chọn size/màu khi bấm "Mua ngay"
    @ManyToOne
    @JoinColumn(name = "ProductID", nullable = false)
    private Product product;

    @Column(name = "AddedDate")
    private LocalDateTime addedDate;
}
