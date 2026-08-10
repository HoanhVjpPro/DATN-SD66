package com.example.datnhathub.controller;

import com.example.datnhathub.entity.Material;
import com.example.datnhathub.entity.Users;
import com.example.datnhathub.repository.MaterialRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/materials")
public class AdminMaterialController {

    @Autowired
    private MaterialRepository materialRepository;

    // GET /admin/materials — Danh sách + form thêm/sửa
    @GetMapping
    public String listMaterials(@RequestParam(required = false) Integer editId, Model model, HttpSession session) {
        Users user = (Users) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        if (!user.getRole().getRoleId().equals(1)) {
            return "access-denied";
        }

        List<Material> materials = materialRepository.findAll();
        model.addAttribute("materials", materials);

        Material material = (editId != null)
                ? materialRepository.findById(editId).orElse(new Material())
                : new Material();
        model.addAttribute("material", material);

        return "admin/materials";
    }

    // POST /admin/materials — Thêm mới HOẶC cập nhật
    @PostMapping
    public String saveMaterial(@RequestParam(required = false) Integer materialId,
                               @RequestParam String materialName,
                               RedirectAttributes ra) {

        Material material = (materialId != null)
                ? materialRepository.findById(materialId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chất liệu ID: " + materialId))
                : new Material();

        material.setMaterialName(materialName);
        materialRepository.save(material);

        ra.addFlashAttribute("success",
                materialId == null ? "Đã thêm chất liệu mới!" : "Đã cập nhật chất liệu!");

        return "redirect:/admin/materials";
    }

    // POST /admin/materials/delete/{id} — Xóa chất liệu
    @PostMapping("/delete/{id}")
    public String deleteMaterial(@PathVariable("id") Integer id, RedirectAttributes ra) {
        try {
            materialRepository.deleteById(id);
            ra.addFlashAttribute("success", "Đã xóa chất liệu!");
        } catch (DataIntegrityViolationException e) {
            ra.addFlashAttribute("error",
                    "Không thể xóa: chất liệu này đang được sản phẩm sử dụng. Hãy đổi chất liệu của sản phẩm trước.");
        }
        return "redirect:/admin/materials";
    }
}