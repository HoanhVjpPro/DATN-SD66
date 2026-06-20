package com.example.datnhathub.repository;

import com.example.datnhathub.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {
    // Kiểm tra tên danh mục đã tồn tại chưa
    boolean existsByCategoryName(String categoryName);
}
