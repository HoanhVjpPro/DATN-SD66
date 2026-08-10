package com.example.datnhathub.repository;

import com.example.datnhathub.entity.WishlistDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface WishlistDetailRepository extends JpaRepository<WishlistDetail, Integer> {

    // Kiểm tra 1 sản phẩm đã có trong wishlist chưa
    Optional<WishlistDetail> findByWishlistWishlistIdAndProductProductId(Integer wishlistId, Integer productId);

    @Modifying
    @Transactional
    @Query("DELETE FROM WishlistDetail wd WHERE wd.wishlist.wishlistId = :wishlistId AND wd.product.productId = :productId")
    void deleteByWishlistIdAndProductId(@Param("wishlistId") Integer wishlistId, @Param("productId") Integer productId);
}
