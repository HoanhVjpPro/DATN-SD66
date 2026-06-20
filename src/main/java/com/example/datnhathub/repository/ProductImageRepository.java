package com.example.datnhathub.repository;

import com.example.datnhathub.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Integer> {
    // Lấy ảnh mặc định của sản phẩm
    Optional<ProductImage> findByProductProductIdAndIsDefaultTrue(Integer productId);

    // Lấy tất cả ảnh của sản phẩm (dùng khi reset isDefault)
    List<ProductImage> findAllByProductProductId(Integer productId);
}
