package com.example.datnhathub.controller;

import com.example.datnhathub.entity.Size;
import com.example.datnhathub.entity.Users;
import com.example.datnhathub.repository.SizeRepository;
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
@RequestMapping("/admin/sizes")
public class AdminSizeController {

    @Autowired
    private SizeRepository sizeRepository;

    // GET /admin/sizes — Danh sách + form thêm/sửa
    @GetMapping
    public String listSizes(@RequestParam(required = false) Integer editId, Model model, HttpSession session) {
        Users user = (Users) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        if (!user.getRole().getRoleId().equals(1)) {
            return "access-denied";
        }

        List<Size> sizes = sizeRepository.findAll();
        model.addAttribute("sizes", sizes);

        Size size = (editId != null)
                ? sizeRepository.findById(editId).orElse(new Size())
                : new Size();
        model.addAttribute("size", size);

        return "admin/sizes";
    }

    // POST /admin/sizes — Thêm mới HOẶC cập nhật (dùng cho trang quản lý Size)
    @PostMapping
    public String saveSize(@RequestParam(required = false) Integer sizeId,
                           @RequestParam String sizeName,
                           RedirectAttributes ra) {

        Size size = (sizeId != null)
                ? sizeRepository.findById(sizeId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy size ID: " + sizeId))
                : new Size();

        size.setSizeName(sizeName);
        sizeRepository.save(size);

        ra.addFlashAttribute("success",
                sizeId == null ? "Đã thêm size mới!" : "Đã cập nhật size!");

        return "redirect:/admin/sizes";
    }

    // POST /admin/sizes/quick-add — Thêm size nhanh ngay trong popup "Tạo biến thể" (AJAX, trả JSON)
    // Nếu tên đã tồn tại (không phân biệt hoa thường) thì trả về bản ghi cũ, không tạo trùng
    @PostMapping("/quick-add")
    @ResponseBody
    public Map<String, Object> quickAddSize(@RequestParam String sizeName) {
        Map<String, Object> result = new HashMap<>();
        String name = sizeName == null ? "" : sizeName.trim();

        if (name.isEmpty()) {
            result.put("success", false);
            result.put("message", "Tên size không được để trống");
            return result;
        }

        Size size = sizeRepository.findBySizeNameIgnoreCase(name)
                .orElseGet(() -> {
                    Size s = new Size();
                    s.setSizeName(name);
                    return sizeRepository.save(s);
                });

        result.put("success", true);
        result.put("sizeId", size.getSizeId());
        result.put("sizeName", size.getSizeName());
        return result;
    }

    // POST /admin/sizes/delete/{id} — Xóa size
    @PostMapping("/delete/{id}")
    public String deleteSize(@PathVariable("id") Integer id, RedirectAttributes ra) {
        try {
            sizeRepository.deleteById(id);
            ra.addFlashAttribute("success", "Đã xóa size!");
        } catch (DataIntegrityViolationException e) {
            ra.addFlashAttribute("error", "Không thể xóa size này!");
        }
        return "redirect:/admin/sizes";
    }
}