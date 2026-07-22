package com.example.datnhathub.controller;

import com.example.datnhathub.entity.Brand;
import com.example.datnhathub.entity.Users;
import com.example.datnhathub.repository.BrandRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/brands")
public class AdminBrandController {

    @Autowired
    private BrandRepository brandRepository;

    // GET /admin/brands — Danh sách + form thêm/sửa
    @GetMapping
    public String listBrands(@RequestParam(required = false) Integer editId, Model model, HttpSession session) {
        Users user =  (Users) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        if (!user.getRole().getRoleId().equals(1)) {
            return "access-denied";
        }
        List<Brand> brands = brandRepository.findAll();
        model.addAttribute("brands", brands);

        Brand brand = (editId != null)
                ? brandRepository.findById(editId).orElse(new Brand())
                : new Brand();
        model.addAttribute("brand", brand);

        return "admin/brand";
    }

    // POST /admin/brands — Thêm mới HOẶC cập nhật
    @PostMapping
    public String saveBrand(@RequestParam(required = false) Integer brandId,
                            @RequestParam String brandName,
                            RedirectAttributes ra) {

        Brand brand = (brandId != null)
                ? brandRepository.findById(brandId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thương hiệu ID: " + brandId))
                : new Brand();

        brand.setBrandName(brandName);
        brandRepository.save(brand);

        ra.addFlashAttribute("success",
                brandId == null ? "Đã thêm thương hiệu mới!" : "Đã cập nhật thương hiệu!");

        return "redirect:/admin/brands";
    }

    // POST /admin/brands/delete/{id} — Xóa thương hiệu
    @PostMapping("/delete/{id}")
    public String deleteBrand(@PathVariable("id") Integer id, RedirectAttributes ra) {
        try {
            brandRepository.deleteById(id);
            ra.addFlashAttribute("success", "Đã xóa thương hiệu!");
        } catch (DataIntegrityViolationException e) {
            ra.addFlashAttribute("error",
                    "Không thể xóa: thương hiệu này đang có sản phẩm. Hãy chuyển sản phẩm sang thương hiệu khác trước.");
        }
        return "redirect:/admin/brands";
    }
}