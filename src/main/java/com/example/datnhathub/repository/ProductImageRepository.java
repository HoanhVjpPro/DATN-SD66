package com.example.datnhathub.repository;

import com.example.datnhathub.entity.ProductDetail;
import com.example.datnhathub.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Integer> {
    Optional<ProductImage> findByProductProductIdAndIsDefaultTrue(Integer productId);

    List<ProductImage> findAllByProductProductId(Integer productId);

    List<ProductImage> findByProductDetailProductDetailId(Integer productDetailId);
}
