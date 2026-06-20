package com.example.datnhathub.repository;

import com.example.datnhathub.entity.ProductDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductDetailRepository extends JpaRepository<ProductDetail, Integer> {
    // Lấy tất cả biến thể theo productId
    List<ProductDetail> findByProductProductId(Integer productId);
}
