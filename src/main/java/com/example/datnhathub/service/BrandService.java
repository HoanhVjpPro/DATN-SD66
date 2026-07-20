package com.example.datnhathub.service;

import com.example.datnhathub.entity.Brand;
import com.example.datnhathub.repository.BrandRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BrandService {
    @Autowired
    private BrandRepository brandRepository;

    public List<Brand> getAll() {
        return brandRepository.findAll();
    }

    public Brand getById(Integer id) {
        return brandRepository.findById(id).orElse(null);
    }

    public Brand save(Brand brand) {
        return brandRepository.save(brand);
    }

    public void delete(Integer id) {
        brandRepository.deleteById(id);
    }
}