package com.example.datnhathub.repository;

import com.example.datnhathub.entity.HatType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HatTypeRepository extends JpaRepository<HatType, Integer> {
    boolean existsByHatTypeName(String hatTypeName);
}