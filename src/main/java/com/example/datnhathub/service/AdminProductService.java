package com.example.datnhathub.service;

import com.example.datnhathub.entity.Category;
import com.example.datnhathub.entity.Product;
import com.example.datnhathub.entity.ProductDetail;
import com.example.datnhathub.entity.ProductImage;
import com.example.datnhathub.repository.CategoryRepository;
import com.example.datnhathub.repository.ProductDetailRepository;
import com.example.datnhathub.repository.ProductImageRepository;
import com.example.datnhathub.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
public class AdminProductService {
    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductDetailRepository productDetailRepository;

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    // ════════════════════════════════════════
    // UC10, UC11 — Lấy tất cả sản phẩm (admin)
    // ════════════════════════════════════════
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    // ════════════════════════════════════════
    // UC10 — Lấy sản phẩm theo ID (dùng cho form edit)
    // ════════════════════════════════════════
    public Product getProductById(Integer id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm ID: " + id));
    }

    // ════════════════════════════════════════
    // UC10, UC11 — Lưu sản phẩm (thêm mới + sửa)
    // ════════════════════════════════════════
    public Product saveProduct(Integer productId,
                               String productName,
                               Integer categoryId,
                               String description,
                               boolean status) {

        // Thêm mới hoặc lấy product đang sửa
        Product product = productId != null
                ? getProductById(productId)
                : new Product();

        product.setProductName(productName);
        product.setDescription(description);
        product.setStatus(status);

        // Set category
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục ID: " + categoryId));
        product.setCategory(category);

        return productRepository.save(product);
    }

    // ════════════════════════════════════════
    // UC12 — Xóa sản phẩm
    // ════════════════════════════════════════
    public void deleteProduct(Integer productId) {
        productRepository.deleteById(productId);
    }

    // ════════════════════════════════════════
    // UC13 — Thêm biến thể
    // ════════════════════════════════════════
    public void addDetail(Integer productId,
                          String size,
                          String color,
                          BigDecimal price,
                          String sku) {

        Product product = getProductById(productId);

        ProductDetail detail = new ProductDetail();
        detail.setProduct(product);
        detail.setSize(size);
        detail.setColor(color);
        detail.setPrice(price);
        detail.setSku(sku);

        productDetailRepository.save(detail);
    }

    // ════════════════════════════════════════
    // UC13 — Xóa biến thể
    // ════════════════════════════════════════
    public void deleteDetail(Integer detailId) {
        productDetailRepository.deleteById(detailId);
    }

    // ════════════════════════════════════════
    // UC13 — Lấy biến thể theo productId
    // ════════════════════════════════════════
    public List<ProductDetail> getDetailsByProductId(Integer productId) {
        return productDetailRepository.findByProductProductId(productId);
    }

    // ════════════════════════════════════════
    // UC14 — Upload ảnh sản phẩm
    // ════════════════════════════════════════
    public void uploadImage(Integer productId,
                            MultipartFile file,
                            boolean isDefault) throws IOException {

        // Lưu ra ngoài project — dễ truy cập hơn
        String uploadDir = System.getProperty("user.dir") + "/uploads/products/";
        String fileName  = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path filePath    = Paths.get(uploadDir + fileName);

        Files.createDirectories(filePath.getParent());
        Files.write(filePath, file.getBytes());

        // Reset isDefault cũ nếu cần
        if (isDefault) {
            List<ProductImage> existingImages =
                    productImageRepository.findAllByProductProductId(productId);
            existingImages.forEach(img -> {
                img.setIsDefault(false);
                productImageRepository.save(img);
            });
        }

        // Lưu vào DB
        Product product  = getProductById(productId);
        ProductImage image = new ProductImage();
        image.setProduct(product);
        image.setImageURL("/uploads/products/" + fileName); // URL truy cập
        image.setIsDefault(isDefault);
        productImageRepository.save(image);
    }
}
