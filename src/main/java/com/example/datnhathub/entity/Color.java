package com.example.datnhathub.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Product_Color")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Color {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ColorID")
    private Integer colorId;

    @Column(name = "ColorName", nullable = false, length = 50)
    private String colorName;

    // Mã màu hex (vd: #C0633A) — tùy chọn, dùng để hiển thị ô màu (swatch) trên giao diện
    @Column(name = "ColorCode", length = 10)
    private String colorCode;
}