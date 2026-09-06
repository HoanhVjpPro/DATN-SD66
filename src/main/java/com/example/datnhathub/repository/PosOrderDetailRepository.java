package com.example.datnhathub.repository;

import com.example.datnhathub.entity.PosOrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PosOrderDetailRepository extends JpaRepository<PosOrderDetail, Integer> {
}