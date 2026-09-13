package com.example.datnhathub.controller;

import com.example.datnhathub.dto.ProductDetailDto;
import com.example.datnhathub.entity.ProductDetail;
import com.example.datnhathub.repository.ProductDetailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductApiController {

    @Autowired
    private ProductDetailRepository productDetailRepository;

    @GetMapping("/{id}/details")
    public List<ProductDetailDto> getProductDetails(@PathVariable("id") Integer id) {
        List<ProductDetail> details = productDetailRepository.findByProductProductId(id);
        return details.stream().map(ProductDetailDto::new).toList();
    }
}