package com.example.datnhathub.repository;

import com.example.datnhathub.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MaterialRepository extends JpaRepository<Material, Integer> {
    boolean existsByMaterialName(String materialName);
}