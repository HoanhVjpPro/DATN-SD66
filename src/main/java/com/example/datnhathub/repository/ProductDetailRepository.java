package com.example.datnhathub.repository;

import com.example.datnhathub.entity.ProductDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ProductDetailRepository extends JpaRepository<ProductDetail, Integer> {
    // Lấy tất cả biến thể theo productId
    List<ProductDetail> findByProductProductId(Integer productId);

    // Kiểm tra SKU đã tồn tại chưa — dùng khi tự sinh SKU để đảm bảo unique
    boolean existsBySku(String sku);

    @Modifying
    @Transactional
    @Query("UPDATE ProductDetail p SET p.stockQuantity = p.stockQuantity - :qty " +
            "WHERE p.productDetailId = :id AND p.stockQuantity >= :qty")
    int decreaseStock(@Param("id") Integer productDetailId, @Param("qty") Integer qty);
}