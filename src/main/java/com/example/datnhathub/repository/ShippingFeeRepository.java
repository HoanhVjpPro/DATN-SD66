package com.example.datnhathub.repository;

import com.example.datnhathub.entity.ShippingFee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShippingFeeRepository extends JpaRepository<ShippingFee, Integer> {

    Optional<ShippingFee> findByProvince(String province);

    Optional<ShippingFee> findByProvinceContainingIgnoreCase(String province);

}