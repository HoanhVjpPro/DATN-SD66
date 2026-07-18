package com.example.datnhathub.repository;

import com.example.datnhathub.entity.Shipper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShipperRepository extends JpaRepository<Shipper, Integer> {
    Optional<Shipper> findByUserUserID(Integer userId);
}