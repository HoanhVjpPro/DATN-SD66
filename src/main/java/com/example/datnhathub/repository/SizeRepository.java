package com.example.datnhathub.repository;

import com.example.datnhathub.entity.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SizeRepository extends JpaRepository<Size, Integer> {
    boolean existsBySizeName(String sizeName);

    // Dùng cho chức năng "Thêm size nhanh" trong popup Tạo biến thể — tránh tạo trùng
    Optional<Size> findBySizeNameIgnoreCase(String sizeName);
}