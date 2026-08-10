package com.example.datnhathub.controller;

import com.example.datnhathub.entity.Product;
import com.example.datnhathub.entity.ProductDetail;
import com.example.datnhathub.entity.Users;
import com.example.datnhathub.repository.CategoryRepository;
import com.example.datnhathub.repository.ProductDetailRepository;
import com.example.datnhathub.repository.ProductImageRepository;
import com.example.datnhathub.repository.ProductRepository;
import com.example.datnhathub.service.AdminProductService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/admin/products")
public class AdminProductController {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductDetailRepository productDetailRepository;

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private AdminProductService adminProductService;

    // GET /admin/products — Danh sách sản phẩm
    @GetMapping
    public String listProducts(Model model, HttpSession session) {
        Users user = (Users) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        if (!user.getRole().getRoleId().equals(1)) {
            return "access-denied";
        }
        List<Product> products = adminProductService.getAllProducts();
        model.addAttribute("products", products);
        return "admin/products"; // templates/admin/products.html
    }

    // GET /admin/products/new — Form thêm mới
    @GetMapping("/new")
    public String newProductForm(Model model) {
        model.addAttribute("product",    new Product());
        model.addAttribute("categories", adminProductService.getAllCategories());
        model.addAttribute("brands",     adminProductService.getAllBrands());
        model.addAttribute("materials",  adminProductService.getAllMaterials());
        model.addAttribute("hatTypes",   adminProductService.getAllHatTypes());
        model.addAttribute("allSizes",   adminProductService.getAllSizes());
        model.addAttribute("allColors",  adminProductService.getAllColors());
        return "admin/product-form";
    }

    // GET /admin/products/edit/{id} — Form sửa
    @GetMapping("/edit/{id}")
    public String editProductForm(@PathVariable Integer id, Model model) {
        Product product = adminProductService.getProductById(id);
        List<ProductDetail> details = adminProductService.getDetailsByProductId(id);

        model.addAttribute("product",    product);
        model.addAttribute("details",    details);
        model.addAttribute("categories", adminProductService.getAllCategories());
        model.addAttribute("brands",     adminProductService.getAllBrands());
        model.addAttribute("materials",  adminProductService.getAllMaterials());
        model.addAttribute("hatTypes",   adminProductService.getAllHatTypes());
        model.addAttribute("allSizes",   adminProductService.getAllSizes());
        model.addAttribute("allColors",  adminProductService.getAllColors());
        return "admin/product-form";
    }

    // POST /admin/products/save — Lưu sản phẩm
    @PostMapping("/save")
    public String saveProduct(@RequestParam(required = false) Integer productId,
                              @RequestParam String productName,
                              @RequestParam Integer categoryId,
                              @RequestParam(required = false) Integer brandId,
                              @RequestParam(required = false) Integer materialId,
                              @RequestParam(required = false) Integer hatTypeId,
                              @RequestParam(required = false, defaultValue = "") String description,
                              @RequestParam(required = false) String status,
                              RedirectAttributes ra) {

        boolean isActive = "true".equals(status);

        Product saved = adminProductService.saveProduct(
                productId, productName, categoryId, brandId, materialId, hatTypeId, description, isActive
        );

        ra.addFlashAttribute("success",
                productId == null ? "Thêm sản phẩm thành công!" : "Cập nhật sản phẩm thành công!"
        );

        // Sau khi lưu → chuyển sang form edit để thêm biến thể + ảnh
        return "redirect:/admin/products/edit/" + saved.getProductId();
    }

    // POST /admin/products/{id}/detail — Thêm 1 biến thể lẻ (giữ lại phòng khi cần). SKU tự sinh, không nhận tay.
    @PostMapping("/{id}/detail")
    public String addDetail(@PathVariable Integer id,
                            @RequestParam String size,
                            @RequestParam String color,
                            @RequestParam BigDecimal price,
                            @RequestParam(defaultValue = "0") Integer stockQuantity,
                            RedirectAttributes ra) {

        adminProductService.addDetail(id, size, color, price, stockQuantity);
        ra.addFlashAttribute("success", "Đã thêm biến thể!");
        return "redirect:/admin/products/edit/" + id;
    }

    // POST /admin/products/{id}/detail/batch — Tạo nhiều biến thể cùng lúc
    // (chọn nhiều Size + nhiều Màu trong popup — lấy từ bảng Product_Size / Product_Color,
    //  1 mức giá & 1 tồn kho ban đầu áp dụng cho tất cả tổ hợp, SKU tự sinh không dấu + đảm bảo unique)
    @PostMapping("/{id}/detail/batch")
    public String addDetailsBatch(@PathVariable Integer id,
                                  @RequestParam(required = false) String sizes,
                                  @RequestParam(required = false) String colors,
                                  @RequestParam BigDecimal price,
                                  @RequestParam(defaultValue = "0") Integer stockQuantity,
                                  RedirectAttributes ra) {

        List<String> sizeList = sizes == null ? List.of() :
                Arrays.stream(sizes.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        List<String> colorList = colors == null ? List.of() :
                Arrays.stream(colors.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();

        if (sizeList.isEmpty() || colorList.isEmpty()) {
            ra.addFlashAttribute("error", "Vui lòng chọn ít nhất 1 Size và 1 Màu!");
            return "redirect:/admin/products/edit/" + id;
        }

        int totalCombos = sizeList.size() * colorList.size();
        int created = adminProductService.createVariantsBatch(id, sizeList, colorList, price, stockQuantity);
        int skipped = totalCombos - created;

        String msg = "Đã tạo " + created + " biến thể mới!";
        if (skipped > 0) {
            msg += " (" + skipped + " tổ hợp đã tồn tại nên bỏ qua)";
        }
        ra.addFlashAttribute("success", msg);
        return "redirect:/admin/products/edit/" + id;
    }

    // POST /admin/products/detail/delete/{detailId} — Xóa 1 biến thể
    @PostMapping("/detail/delete/{detailId}")
    public String deleteDetail(@PathVariable Integer detailId,
                               @RequestParam Integer productId,
                               RedirectAttributes ra) {

        adminProductService.deleteDetail(detailId);
        ra.addFlashAttribute("success", "Đã xóa biến thể!");
        return "redirect:/admin/products/edit/" + productId;
    }

    // POST /admin/products/detail/delete-batch — Xóa nhanh nhiều biến thể đã chọn (checkbox trong bảng)
    @PostMapping("/detail/delete-batch")
    public String deleteDetailsBatch(@RequestParam Integer productId,
                                     @RequestParam(required = false) List<Integer> detailIds,
                                     RedirectAttributes ra) {

        if (detailIds == null || detailIds.isEmpty()) {
            ra.addFlashAttribute("error", "Vui lòng chọn ít nhất 1 biến thể để xóa!");
            return "redirect:/admin/products/edit/" + productId;
        }

        int deleted = adminProductService.deleteDetails(detailIds);
        ra.addFlashAttribute("success", "Đã xóa " + deleted + " biến thể đã chọn!");
        return "redirect:/admin/products/edit/" + productId;
    }

    // POST /admin/products/{id}/image — Upload ảnh (dùng chung cho ảnh sản phẩm & ảnh riêng từng biến thể)
    @PostMapping("/{id}/image")
    public String uploadImage(@PathVariable Integer id,
                              @RequestParam MultipartFile file,
                              @RequestParam(required = false) String isDefault,
                              @RequestParam(required = false) Integer productDetailId,
                              RedirectAttributes ra) {

        try {
            boolean setDefault = "true".equals(isDefault);
            adminProductService.uploadImage(id, file, setDefault, productDetailId);
            ra.addFlashAttribute("success", "Upload ảnh thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Upload thất bại: " + e.getMessage());
        }

        return "redirect:/admin/products/edit/" + id;
    }

    // UC12 — Ẩn / Hiện sản phẩm (an toàn, không xóa dữ liệu)
    @PostMapping("/toggle/{id}")
    public String toggleProductStatus(@PathVariable Integer id, RedirectAttributes ra) {
        adminProductService.toggleProductStatus(id);
        ra.addFlashAttribute("success", "Đã đổi trạng thái sản phẩm!");
        return "redirect:/admin/products";
    }

    // UC14 — Xóa ảnh sản phẩm
    @PostMapping("/image/delete/{imageId}")
    public String deleteImage(@PathVariable Integer imageId,
                              @RequestParam Integer productId,
                              RedirectAttributes ra) {
        adminProductService.deleteImage(imageId);
        ra.addFlashAttribute("success", "Đã xóa ảnh!");
        return "redirect:/admin/products/edit/" + productId;
    }


    // ====== deleteProduct() — bắt lỗi khóa ngoại để không crash app ======
    @PostMapping("/delete/{id}")
    public String deleteProduct(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            adminProductService.deleteProduct(id);
            ra.addFlashAttribute("success", "Đã xóa sản phẩm!");
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Sản phẩm đã có biến thể nằm trong đơn hàng -> không thể xóa cứng
            ra.addFlashAttribute("error", "Không thể xóa: sản phẩm đã có trong đơn hàng. Hãy ẩn sản phẩm để thay thế.");
        }
        return "redirect:/admin/products";
    }

    // POST /admin/products/detail/update/{detailId} — Sửa biến thể (SKU không được sửa)
    @PostMapping("/detail/update/{detailId}")
    public String updateDetail(@PathVariable Integer detailId,
                               @RequestParam String size,
                               @RequestParam String color,
                               @RequestParam BigDecimal price,
                               @RequestParam Integer stockQuantity,
                               @RequestParam Integer productId,
                               RedirectAttributes ra) {

        adminProductService.updateDetail(detailId, size, color, price, stockQuantity);
        ra.addFlashAttribute("success", "Đã cập nhật biến thể!");
        return "redirect:/admin/products/edit/" + productId;
    }
}