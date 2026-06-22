package com.example.datnhathub.repository;

import com.example.datnhathub.entity.CartDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartDetailRepository extends JpaRepository<CartDetail, Integer> {
    // Kiểm tra biến thể này đã có trong giỏ chưa, để cộng dồn số lượng thay vì tạo dòng mới
    Optional<CartDetail> findByCartCartIdAndProductDetailProductDetailId(Integer cartId, Integer productDetailId);
}
