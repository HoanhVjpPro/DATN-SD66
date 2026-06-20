package com.example.datnhathub.repository;


import com.example.datnhathub.dto.ProductDto;
import com.example.datnhathub.entity.Category;
import com.example.datnhathub.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {
    // ── UC06: Danh sách sản phẩm + lọc + tìm kiếm + phân trang ──
    @Query("""
        SELECT DISTINCT p FROM Product p
        JOIN p.category c
        LEFT JOIN p.details d
        WHERE p.status = true
          AND (:keyword IS NULL OR LOWER(p.productName) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:categoryId IS NULL OR c.categoryId = :categoryId)
          AND (:minPrice IS NULL OR d.price >= :minPrice)
          AND (:maxPrice IS NULL OR d.price <= :maxPrice)
    """)
    Page<Product> findByFilters(
            @Param("keyword")    String keyword,
            @Param("categoryId") Integer categoryId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice")   BigDecimal maxPrice,
            Pageable pageable
    );

    // ── UC08: Tìm kiếm theo tên ──
    @Query("""
        SELECT p FROM Product p
        WHERE p.status = true
          AND LOWER(p.productName) LIKE LOWER(CONCAT('%', :keyword, '%'))
    """)
    Page<Product> searchByName(@Param("keyword") String keyword, Pageable pageable);

    // ── UC09: Lọc theo danh mục ──
    Page<Product> findByCategoryCategoryIdAndStatusTrue(Integer categoryId, Pageable pageable);


}
