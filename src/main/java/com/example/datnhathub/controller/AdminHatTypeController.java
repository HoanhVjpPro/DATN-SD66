package com.example.datnhathub.controller;

import com.example.datnhathub.entity.HatType;
import com.example.datnhathub.entity.Users;
import com.example.datnhathub.repository.HatTypeRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/hat-types")
public class AdminHatTypeController {

    @Autowired
    private HatTypeRepository hatTypeRepository;

    // GET /admin/hat-types — Danh sách + form thêm/sửa
    @GetMapping
    public String listHatTypes(@RequestParam(required = false) Integer editId, Model model, HttpSession session) {
        Users user = (Users) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        if (!user.getRole().getRoleId().equals(1)) {
            return "access-denied";
        }

        List<HatType> hatTypes = hatTypeRepository.findAll();
        model.addAttribute("hatTypes", hatTypes);

        HatType hatType = (editId != null)
                ? hatTypeRepository.findById(editId).orElse(new HatType())
                : new HatType();
        model.addAttribute("hatType", hatType);

        return "admin/hat-types";
    }

    // POST /admin/hat-types — Thêm mới HOẶC cập nhật
    @PostMapping
    public String saveHatType(@RequestParam(required = false) Integer hatTypeId,
                              @RequestParam String hatTypeName,
                              RedirectAttributes ra) {

        HatType hatType = (hatTypeId != null)
                ? hatTypeRepository.findById(hatTypeId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy kiểu mũ ID: " + hatTypeId))
                : new HatType();

        hatType.setHatTypeName(hatTypeName);
        hatTypeRepository.save(hatType);

        ra.addFlashAttribute("success",
                hatTypeId == null ? "Đã thêm kiểu mũ mới!" : "Đã cập nhật kiểu mũ!");

        return "redirect:/admin/hat-types";
    }

    // POST /admin/hat-types/delete/{id} — Xóa kiểu mũ
    @PostMapping("/delete/{id}")
    public String deleteHatType(@PathVariable("id") Integer id, RedirectAttributes ra) {
        try {
            hatTypeRepository.deleteById(id);
            ra.addFlashAttribute("success", "Đã xóa kiểu mũ!");
        } catch (DataIntegrityViolationException e) {
            ra.addFlashAttribute("error",
                    "Không thể xóa: kiểu mũ này đang được sản phẩm sử dụng. Hãy đổi kiểu mũ của sản phẩm trước.");
        }
        return "redirect:/admin/hat-types";
    }
}