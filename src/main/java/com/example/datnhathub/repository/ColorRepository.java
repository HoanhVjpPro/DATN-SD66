package com.example.datnhathub.repository;

import com.example.datnhathub.entity.Color;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ColorRepository extends JpaRepository<Color, Integer> {
    boolean existsByColorName(String colorName);

    // Dùng cho chức năng "Thêm màu nhanh" trong popup Tạo biến thể — tránh tạo trùng
    Optional<Color> findByColorNameIgnoreCase(String colorName);
}