package com.example.datnhathub.service;

import com.example.datnhathub.entity.*;
import com.example.datnhathub.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
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

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private HatTypeRepository hatTypeRepository;

    @Autowired
    private SizeRepository sizeRepository;

    @Autowired
    private ColorRepository colorRepository;

    public List<Brand> getAllBrands() {
        return brandRepository.findAll();
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public List<Material> getAllMaterials() {
        return materialRepository.findAll();
    }

    public List<HatType> getAllHatTypes() {
        return hatTypeRepository.findAll();
    }

    // Danh sách Size master data — dùng đổ vào popup "Tạo biến thể"
    public List<Size> getAllSizes() {
        return sizeRepository.findAll();
    }

    // Danh sách Màu master data — dùng đổ vào popup "Tạo biến thể"
    public List<Color> getAllColors() {
        return colorRepository.findAll();
    }

    // Lấy tất cả sản phẩm (admin)
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    // Lấy sản phẩm theo ID (dùng cho form edit)
    public Product getProductById(Integer id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm ID: " + id));
    }

    // Lưu sản phẩm (thêm mới + sửa)
    public Product saveProduct(Integer productId, String productName, Integer categoryId,
                               Integer brandId, Integer materialId, Integer hatTypeId,
                               String description, boolean isActive) {
        Product product = (productId != null)
                ? productRepository.findById(productId).orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm ID: " + productId))
                : new Product();

        product.setProductName(productName);
        product.setDescription(description);
        product.setStatus(isActive);

        Category category = categoryRepository.findById(categoryId).orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục ID: " + categoryId));
        product.setCategory(category);

        if (brandId != null) {
            Brand brand = brandRepository.findById(brandId).orElseThrow(() -> new RuntimeException("Không tìm thấy thương hiệu ID: " + brandId));
            product.setBrand(brand);
        } else {
            product.setBrand(null);
        }

        if (materialId != null) {
            Material material = materialRepository.findById(materialId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy chất liệu ID: " + materialId));
            product.setMaterial(material);
        } else {
            product.setMaterial(null);
        }

        if (hatTypeId != null) {
            HatType hatType = hatTypeRepository.findById(hatTypeId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy kiểu mũ ID: " + hatTypeId));
            product.setHatType(hatType);
        } else {
            product.setHatType(null);
        }

        return productRepository.save(product);
    }

    // Xóa sản phẩm
    public void deleteProduct(Integer productId) {
        productRepository.deleteById(productId);
    }

    // Thêm 1 biến thể lẻ — nhận sizeId/colorId (FK thật tới Product_Size/Product_Color), SKU luôn tự sinh
    public void addDetail(Integer productId,
                          Integer sizeId,
                          Integer colorId,
                          BigDecimal price,
                          Integer stockQuantity) {

        Product product = getProductById(productId);
        Size size = sizeRepository.findById(sizeId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy size ID: " + sizeId));
        Color color = colorRepository.findById(colorId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy màu ID: " + colorId));

        ProductDetail detail = new ProductDetail();
        detail.setProduct(product);
        detail.setSizeEntity(size);
        detail.setColorEntity(color);
        detail.setPrice(price);
        detail.setSku(buildSku(product, size.getSizeName(), color.getColorName()));
        detail.setStockQuantity(stockQuantity);

        productDetailRepository.save(detail);
    }

    // Tạo nhiều biến thể cùng lúc theo tổ hợp (Size x Màu, nhận sizeIds/colorIds), dùng chung 1 giá + 1 tồn kho ban đầu.
    // Bỏ qua các tổ hợp Size+Màu đã tồn tại sẵn cho sản phẩm này (tránh tạo trùng).
    public int createVariantsBatch(Integer productId, List<Integer> sizeIds, List<Integer> colorIds,
                                   BigDecimal price, Integer stockQuantity) {
        Product product = getProductById(productId);
        List<ProductDetail> existing = productDetailRepository.findByProductProductId(productId);

        int created = 0;
        for (Integer sizeId : sizeIds) {
            if (sizeId == null) continue;
            Size size = sizeRepository.findById(sizeId).orElse(null);
            if (size == null) continue;

            for (Integer colorId : colorIds) {
                if (colorId == null) continue;
                Color color = colorRepository.findById(colorId).orElse(null);
                if (color == null) continue;

                boolean alreadyExists = existing.stream().anyMatch(d ->
                        d.getSizeEntity() != null && d.getColorEntity() != null
                                && sizeId.equals(d.getSizeEntity().getSizeId())
                                && colorId.equals(d.getColorEntity().getColorId()));
                if (alreadyExists) continue;

                ProductDetail detail = new ProductDetail();
                detail.setProduct(product);
                detail.setSizeEntity(size);
                detail.setColorEntity(color);
                detail.setPrice(price);
                detail.setSku(buildSku(product, size.getSizeName(), color.getColorName()));
                detail.setStockQuantity(stockQuantity == null ? 0 : stockQuantity);
                productDetailRepository.save(detail);
                created++;
            }
        }
        return created;
    }

    // Sinh SKU: KHÔNG dấu tiếng Việt, chỉ gồm chữ hoa/số, và luôn đảm bảo UNIQUE toàn hệ thống
    // (nếu trùng thì tự thêm hậu tố -2, -3... cho tới khi không còn trùng)
    private String buildSku(Product product, String size, String color) {
        String sizePart = toSkuToken(size);
        String colorPart = toSkuToken(color);
        String base = "SP" + product.getProductId()
                + (sizePart.isEmpty() ? "" : "-" + sizePart)
                + (colorPart.isEmpty() ? "" : "-" + colorPart);

        String candidate = base;
        int suffix = 2;
        while (productDetailRepository.existsBySku(candidate)) {
            candidate = base + "-" + suffix;
            suffix++;
        }
        return candidate;
    }

    // Bỏ dấu tiếng Việt + chỉ giữ lại chữ/số, viết hoa toàn bộ — dùng để ghép vào SKU
    private String toSkuToken(String input) {
        if (input == null) return "";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String noAccents = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        noAccents = noAccents.replace('Đ', 'D').replace('đ', 'd');
        return noAccents.toUpperCase().replaceAll("[^A-Z0-9]", "");
    }

    // Xóa biến thể
    public void deleteDetail(Integer detailId) {
        productDetailRepository.deleteById(detailId);
    }

    // Xóa nhiều biến thể cùng lúc (chọn nhanh nhiều dòng rồi xóa)
    @Transactional
    public int deleteDetails(List<Integer> detailIds) {
        List<Integer> existingIds = detailIds.stream()
                .filter(productDetailRepository::existsById)
                .toList();
        productDetailRepository.deleteAllById(existingIds);
        return existingIds.size();
    }

    // Lấy biến thể theo productId
    public List<ProductDetail> getDetailsByProductId(Integer productId) {
        return productDetailRepository.findByProductProductId(productId);
    }

    // Upload ảnh sản phẩm (có thể gắn cho 1 biến thể cụ thể)
    public void uploadImage(Integer productId,
                            MultipartFile file,
                            boolean isDefault,
                            Integer productDetailId) throws IOException {

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

        if (productDetailId != null) {
            ProductDetail detail = productDetailRepository.findById(productDetailId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy biến thể ID: " + productDetailId));
            image.setProductDetail(detail);
        }

        productImageRepository.save(image);
    }

    // Ẩn / Hiện sản phẩm
    public void toggleProductStatus(Integer productId) {
        Product product = getProductById(productId);
        product.setStatus(!Boolean.TRUE.equals(product.getStatus()));
        productRepository.save(product);
    }

    // Xóa ảnh sản phẩm
    public void deleteImage(Integer imageId) {
        productImageRepository.deleteById(imageId);
    }

    // UC13 — Sửa biến thể (SKU KHÔNG được sửa — giữ nguyên giá trị đã sinh lúc tạo), nhận sizeId/colorId (FK thật)
    public void updateDetail(Integer detailId,
                             Integer sizeId,
                             Integer colorId,
                             BigDecimal price,
                             Integer stockQuantity) {

        ProductDetail detail = productDetailRepository.findById(detailId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy biến thể ID: " + detailId));

        Size size = sizeRepository.findById(sizeId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy size ID: " + sizeId));
        Color color = colorRepository.findById(colorId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy màu ID: " + colorId));

        detail.setSizeEntity(size);
        detail.setColorEntity(color);
        detail.setPrice(price);
        detail.setStockQuantity(stockQuantity);
        // SKU: không đổi

        productDetailRepository.save(detail);
    }
}