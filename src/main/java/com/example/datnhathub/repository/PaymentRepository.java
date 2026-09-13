package com.example.datnhathub.repository;

import com.example.datnhathub.entity.Payment;
import com.example.datnhathub.entity.Orders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    Optional<Payment> findByOrder(Orders order);
    Payment findByOrderOrderId(Integer orderId);
}