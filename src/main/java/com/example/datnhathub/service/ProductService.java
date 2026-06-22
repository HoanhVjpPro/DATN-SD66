package com.example.datnhathub.service;

import com.example.datnhathub.dto.ProductDto;
import com.example.datnhathub.entity.Category;
import com.example.datnhathub.entity.Product;
import com.example.datnhathub.entity.ProductDetail;
import com.example.datnhathub.repository.CategoryRepository;
import com.example.datnhathub.repository.ProductImageRepository;
import com.example.datnhathub.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Service
public class ProductService {
    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductImageRepository productImageRepository;

    public Page<ProductDto> getProducts(String keyword,
                                        Integer categoryId,
                                        BigDecimal minPrice,
                                        BigDecimal maxPrice,
                                        String sort,
                                        int page) {

        // Xác định sắp xếp
        Sort sorting = switch (sort == null ? "default" : sort) {
            case "price-asc"  -> Sort.by("details.price").ascending();
            case "price-desc" -> Sort.by("details.price").descending();
            case "newest"     -> Sort.by("productId").descending();
            default           -> Sort.by("productId").ascending();
        };

        // Phân trang: 12 sản phẩm / trang
        Pageable pageable = PageRequest.of(page, 12, sorting);

        // Query DB
        Page<Product> productPage = productRepository.findByFilters(
                keyword, categoryId, minPrice, maxPrice, pageable
        );

        // Chuyển Entity → DTO
        List<ProductDto> dtos = productPage.getContent()
                .stream()
                .map(this::toDTO)
                .toList();

        return new PageImpl<>(dtos, pageable, productPage.getTotalElements());
    }

    // ════════════════════════════════════════
    // Lấy tất cả danh mục (cho filter sidebar)
    // ════════════════════════════════════════
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    // ════════════════════════════════════════
    // Lấy chi tiết 1 sản phẩm (UC07)
    // ════════════════════════════════════════
    public Product getProductById(Integer productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm ID: " + productId));
    }

    // ════════════════════════════════════════
    // Helper: Chuyển Product → ProductDTO
    // ════════════════════════════════════════
    private ProductDto toDTO(Product product) {
        ProductDto dto = new ProductDto();
        dto.setProductId(product.getProductId());
        dto.setProductName(product.getProductName());
        dto.setStatus(product.getStatus());

        // Tên danh mục
        if (product.getCategory() != null) {
            dto.setCategoryName(product.getCategory().getCategoryName());
        }

        // Ảnh mặc định
        if (product.getImages() != null) {
            product.getImages().stream()
                    .filter(img -> Boolean.TRUE.equals(img.getIsDefault()))
                    .findFirst()
                    .ifPresent(img -> dto.setDefaultImage(img.getImageURL()));

            // Nếu không có ảnh mặc định → lấy ảnh đầu tiên
            if (dto.getDefaultImage() == null && !product.getImages().isEmpty()) {
                dto.setDefaultImage(product.getImages().get(0).getImageURL());
            }
        }

        // Giá thấp nhất trong các biến thể
        if (product.getDetails() != null && !product.getDetails().isEmpty()) {
            BigDecimal minPrice = product.getDetails().stream()
                    .map(ProductDetail::getPrice)
                    .filter(p -> p != null)
                    .min(Comparator.naturalOrder())
                    .orElse(BigDecimal.ZERO);
            dto.setMinPrice(minPrice);
        }

        return dto;
    }

    public String getDefaultImageUrl(Product product) {
        if (product == null || product.getImages() == null || product.getImages().isEmpty()) {
            return "/images/no-image.jpg";
        }
        return product.getImages().stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsDefault()))
                .map(img -> img.getImageURL())
                .findFirst()
                .orElseGet(() -> product.getImages().get(0).getImageURL());
    }

    // Dùng cho trang chủ (HomeController) - lấy N sản phẩm mới nhất, còn đang bán (status=true)

    public List<ProductDto> getFeaturedProducts(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("productId").descending());

        Page<Product> productPage = productRepository.findByFilters(
                null, null, null, null, pageable
        );

        return productPage.getContent()
                .stream()
                .map(this::toDTO) // tái sử dụng method toDTO() private đã có sẵn trong class
                .toList();
    }
}
