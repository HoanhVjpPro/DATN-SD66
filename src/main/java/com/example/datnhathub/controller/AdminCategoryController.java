package com.example.datnhathub.controller;

import com.example.datnhathub.entity.Category;
import com.example.datnhathub.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/categories")
public class AdminCategoryController {

    @Autowired
    private CategoryRepository categoryRepository;

    // GET /admin/categories — Danh sách + form thêm/sửa
    // → templates/admin/categories.html
    @GetMapping
    public String listCategories(@RequestParam(required = false) Integer editId, Model model) {
        List<Category> categories = categoryRepository.findAll();
        model.addAttribute("categories", categories);

        // Nếu có ?editId=X -> load category đó lên form để sửa; ngược lại form trống (thêm mới)
        Category category = (editId != null)
                ? categoryRepository.findById(editId).orElse(new Category())
                : new Category();
        model.addAttribute("category", category);

        return "admin/categories";
    }

    // POST /admin/categories — Thêm mới HOẶC cập nhật
    // (form trong categories.html luôn gửi categoryId ẩn: null = thêm mới, có giá trị = sửa)
    @PostMapping
    public String saveCategory(@RequestParam(required = false) Integer categoryId,
                               @RequestParam String categoryName,
                               RedirectAttributes ra) {

        Category category = (categoryId != null)
                ? categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục ID: " + categoryId))
                : new Category();

        category.setCategoryName(categoryName);
        categoryRepository.save(category);

        ra.addFlashAttribute("success",
                categoryId == null ? "Đã thêm danh mục mới!" : "Đã cập nhật danh mục!");

        return "redirect:/admin/categories";
    }

    // POST /admin/categories/delete/{id} — Xóa danh mục
    @PostMapping("/delete/{id}")
    public String deleteCategory(@PathVariable("id") Integer id, RedirectAttributes ra) {
        try {
            categoryRepository.deleteById(id);
            ra.addFlashAttribute("success", "Đã xóa danh mục!");
        } catch (DataIntegrityViolationException e) {
            // Danh mục đang được Product tham chiếu (FK_Product_Category) -> không thể xóa
            ra.addFlashAttribute("error",
                    "Không thể xóa: danh mục này đang có sản phẩm. Hãy chuyển sản phẩm sang danh mục khác trước.");
        }
        return "redirect:/admin/categories";
    }
}