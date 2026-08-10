package com.example.datnhathub.controller;

import com.example.datnhathub.entity.Color;
import com.example.datnhathub.entity.Users;
import com.example.datnhathub.repository.ColorRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/colors")
public class AdminColorController {

    @Autowired
    private ColorRepository colorRepository;

    // GET /admin/colors — Danh sách + form thêm/sửa
    @GetMapping
    public String listColors(@RequestParam(required = false) Integer editId, Model model, HttpSession session) {
        Users user = (Users) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        if (!user.getRole().getRoleId().equals(1)) {
            return "access-denied";
        }

        List<Color> colors = colorRepository.findAll();
        model.addAttribute("colors", colors);

        Color color = (editId != null)
                ? colorRepository.findById(editId).orElse(new Color())
                : new Color();
        model.addAttribute("color", color);

        return "admin/colors";
    }

    // POST /admin/colors — Thêm mới HOẶC cập nhật (dùng cho trang quản lý Màu)
    @PostMapping
    public String saveColor(@RequestParam(required = false) Integer colorId,
                            @RequestParam String colorName,
                            @RequestParam(required = false) String colorCode,
                            RedirectAttributes ra) {

        Color color = (colorId != null)
                ? colorRepository.findById(colorId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy màu ID: " + colorId))
                : new Color();

        color.setColorName(colorName);
        color.setColorCode((colorCode == null || colorCode.isBlank()) ? null : colorCode.trim());
        colorRepository.save(color);

        ra.addFlashAttribute("success",
                colorId == null ? "Đã thêm màu mới!" : "Đã cập nhật màu!");

        return "redirect:/admin/colors";
    }

    // POST /admin/colors/quick-add — Thêm màu nhanh ngay trong popup "Tạo biến thể" (AJAX, trả JSON)
    // Nếu tên đã tồn tại (không phân biệt hoa thường) thì trả về bản ghi cũ, không tạo trùng
    @PostMapping("/quick-add")
    @ResponseBody
    public Map<String, Object> quickAddColor(@RequestParam String colorName,
                                             @RequestParam(required = false) String colorCode) {
        Map<String, Object> result = new HashMap<>();
        String name = colorName == null ? "" : colorName.trim();

        if (name.isEmpty()) {
            result.put("success", false);
            result.put("message", "Tên màu không được để trống");
            return result;
        }

        Color color = colorRepository.findByColorNameIgnoreCase(name)
                .orElseGet(() -> {
                    Color c = new Color();
                    c.setColorName(name);
                    c.setColorCode((colorCode == null || colorCode.isBlank()) ? null : colorCode.trim());
                    return colorRepository.save(c);
                });

        result.put("success", true);
        result.put("colorId", color.getColorId());
        result.put("colorName", color.getColorName());
        result.put("colorCode", color.getColorCode());
        return result;
    }

    // POST /admin/colors/delete/{id} — Xóa màu
    @PostMapping("/delete/{id}")
    public String deleteColor(@PathVariable("id") Integer id, RedirectAttributes ra) {
        try {
            colorRepository.deleteById(id);
            ra.addFlashAttribute("success", "Đã xóa màu!");
        } catch (DataIntegrityViolationException e) {
            ra.addFlashAttribute("error", "Không thể xóa màu này!");
        }
        return "redirect:/admin/colors";
    }
}