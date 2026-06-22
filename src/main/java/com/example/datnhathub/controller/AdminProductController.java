package com.example.datnhathub.controller;

import com.example.datnhathub.entity.Product;
import com.example.datnhathub.entity.ProductDetail;
import com.example.datnhathub.repository.CategoryRepository;
import com.example.datnhathub.repository.ProductDetailRepository;
import com.example.datnhathub.repository.ProductImageRepository;
import com.example.datnhathub.repository.ProductRepository;
import com.example.datnhathub.service.AdminProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/admin/products")
public class AdminProductController {

    // Phải có đủ 4 dòng này
    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductDetailRepository productDetailRepository;

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private CategoryRepository categoryRepository;  // ← kiểm tra dòng này có chưa

    @Autowired
    private AdminProductService adminProductService;

    // ════════════════════════════════════════
    // GET /admin/products — Danh sách sản phẩm
    // → products.html
    // ════════════════════════════════════════
    @GetMapping
    public String listProducts(Model model) {
        List<Product> products = adminProductService.getAllProducts();
        model.addAttribute("products", products);
        return "admin/products"; // templates/admin/products.html
    }

    // ════════════════════════════════════════
    // GET /admin/products/new — Form thêm mới
    // → product-form.html (product rỗng)
    // ════════════════════════════════════════
    @GetMapping("/new")
    public String newProductForm(Model model) {
        model.addAttribute("product",    new Product());
        model.addAttribute("categories", adminProductService.getAllCategories());
        return "admin/product-form";
    }

    // ════════════════════════════════════════
    // GET /admin/products/edit/{id} — Form sửa
    // → product-form.html (product có dữ liệu)
    // ════════════════════════════════════════
    @GetMapping("/edit/{id}")
    public String editProductForm(@PathVariable Integer id, Model model) {
        Product product = adminProductService.getProductById(id);
        List<ProductDetail> details = adminProductService.getDetailsByProductId(id);

        model.addAttribute("product",    product);
        model.addAttribute("details",    details);
        model.addAttribute("categories", adminProductService.getAllCategories());
        return "admin/product-form";
    }

    // ════════════════════════════════════════
    // POST /admin/products/save — Lưu sản phẩm
    // (dùng cho cả thêm mới lẫn sửa)
    // ════════════════════════════════════════
    @PostMapping("/save")
    public String saveProduct(@RequestParam(required = false) Integer productId,
                              @RequestParam String productName,
                              @RequestParam Integer categoryId,
                              @RequestParam(required = false, defaultValue = "") String description,
                              @RequestParam(required = false) String status,
                              RedirectAttributes ra) {

        boolean isActive = "true".equals(status);

        Product saved = adminProductService.saveProduct(
                productId, productName, categoryId, description, isActive
        );

        ra.addFlashAttribute("success",
                productId == null ? "Thêm sản phẩm thành công!" : "Cập nhật sản phẩm thành công!"
        );

        // Sau khi lưu → chuyển sang form edit để thêm biến thể + ảnh
        return "redirect:/admin/products/edit/" + saved.getProductId();
    }

    // ════════════════════════════════════════
    // POST /admin/products/{id}/detail — Thêm biến thể
    // ════════════════════════════════════════
    @PostMapping("/{id}/detail")
    public String addDetail(@PathVariable Integer id,
                            @RequestParam String size,
                            @RequestParam String color,
                            @RequestParam BigDecimal price,
                            @RequestParam String sku,
                            @RequestParam Integer stockQuantity,   // FIX: nhận thêm tồn kho từ form
                            RedirectAttributes ra) {

        adminProductService.addDetail(id, size, color, price, sku, stockQuantity);
        ra.addFlashAttribute("success", "Đã thêm biến thể!");
        return "redirect:/admin/products/edit/" + id;
    }

    // ════════════════════════════════════════
    // POST /admin/products/detail/delete/{detailId} — Xóa biến thể
    // ════════════════════════════════════════
    @PostMapping("/detail/delete/{detailId}")
    public String deleteDetail(@PathVariable Integer detailId,
                               @RequestParam Integer productId,
                               RedirectAttributes ra) {

        adminProductService.deleteDetail(detailId);
        ra.addFlashAttribute("success", "Đã xóa biến thể!");
        return "redirect:/admin/products/edit/" + productId;
    }

    // ════════════════════════════════════════
    // POST /admin/products/{id}/image — Upload ảnh
    // ════════════════════════════════════════
    @PostMapping("/{id}/image")
    public String uploadImage(@PathVariable Integer id,
                              @RequestParam MultipartFile file,
                              @RequestParam(required = false) String isDefault,
                              RedirectAttributes ra) {

        try {
            boolean setDefault = "true".equals(isDefault);
            adminProductService.uploadImage(id, file, setDefault);
            ra.addFlashAttribute("success", "Upload ảnh thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Upload thất bại: " + e.getMessage());
        }

        return "redirect:/admin/products/edit/" + id;
    }

    // ════════════════════════════════════════
// UC12 — Ẩn / Hiện sản phẩm (an toàn, không xóa dữ liệu)
// POST /admin/products/toggle/{id}
// ════════════════════════════════════════
    @PostMapping("/toggle/{id}")
    public String toggleProductStatus(@PathVariable Integer id, RedirectAttributes ra) {
        adminProductService.toggleProductStatus(id);
        ra.addFlashAttribute("success", "Đã đổi trạng thái sản phẩm!");
        return "redirect:/admin/products";
    }

    // ════════════════════════════════════════
// UC14 — Xóa ảnh sản phẩm
// POST /admin/products/image/delete/{imageId}
// ════════════════════════════════════════
    @PostMapping("/image/delete/{imageId}")
    public String deleteImage(@PathVariable Integer imageId,
                              @RequestParam Integer productId,
                              RedirectAttributes ra) {
        adminProductService.deleteImage(imageId);
        ra.addFlashAttribute("success", "Đã xóa ảnh!");
        return "redirect:/admin/products/edit/" + productId;
    }


    // ====== SỬA LẠI deleteProduct() hiện có — bắt lỗi khóa ngoại để không crash app ======
    @PostMapping("/delete/{id}")
    public String deleteProduct(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            adminProductService.deleteProduct(id);
            ra.addFlashAttribute("success", "Đã xóa sản phẩm!");
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Sản phẩm đã có biến thể nằm trong đơn hàng -> không thể xóa cứng
            ra.addFlashAttribute("error",
                    "Không thể xóa: sản phẩm đã có trong đơn hàng. Hãy dùng nút Ẩn để ngừng bán thay thế.");
        }
        return "redirect:/admin/products";
    }
}
