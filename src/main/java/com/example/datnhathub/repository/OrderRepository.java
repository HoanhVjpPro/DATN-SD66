package com.example.datnhathub.repository;

import com.example.datnhathub.entity.Orders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Orders, Integer> {
    List<Orders> findByCustomerCustomerIdOrderByOrderDateDesc(Integer customerId);
    List<Orders> findAllByOrderByOrderDateDesc();
    long countByStatus(String status);

    // Lấy tất cả đơn có yêu cầu trả hàng
    List<Orders> findByReturnStatusNotNull();

    // Lấy theo trạng thái trả hàng
    List<Orders> findByReturnStatusOrderByReturnDateDesc(String returnStatus);
}
