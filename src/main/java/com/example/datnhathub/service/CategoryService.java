package com.example.datnhathub.service;

import com.example.datnhathub.entity.Category;
import com.example.datnhathub.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {
    @Autowired
    private CategoryRepository categoryRepository;

    public List<Category> getAll() {
        return categoryRepository.findAll();
    }

    public Category getById(Integer id) {
        return categoryRepository.findById(id).orElse(null);
    }

    public Category save(Category category) {
        return categoryRepository.save(category);
    }

    public void delete(Integer id) {
        categoryRepository.deleteById(id);
    }

    public long countProducts(Category category) {
        if (category.getProducts() == null) return 0;
        return category.getProducts().stream()
                .filter(p -> Boolean.TRUE.equals(p.getStatus()))
                .count();
    }
}
