package com.example.datnhathub.repository;

import com.example.datnhathub.entity.PosCustomer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PosCustomerRepository extends JpaRepository<PosCustomer, Integer> {
    Optional<PosCustomer> findByPhone(String phone);
}