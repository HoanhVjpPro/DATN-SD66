package com.example.datnhathub.controller;

import com.example.datnhathub.entity.Users;
import com.example.datnhathub.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProfileController {
    @Autowired
    private UserService userService;
//bro tri
    // Xem thông tin cá nhân
    @GetMapping("/profile")
    public String viewProfile(HttpSession session, Model model) {
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        Users user = userService.getById(userId);
        model.addAttribute("user", user);
        return "profile";
    }

    // Cập nhật thông tin cá nhân
    @PostMapping("/profile/update")
    public String updateProfile(@RequestParam String email,
                                @RequestParam String phone,
                                HttpSession session,
                                RedirectAttributes ra) {
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) return "redirect:/login";

        userService.updateProfile(userId, email, phone);
        ra.addFlashAttribute("success", "Cập nhật thông tin thành công!");
        return "redirect:/profile";
    }

    // Đổi mật khẩu
    @PostMapping("/profile/change-password")
    public String changePassword(@RequestParam String oldPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 HttpSession session,
                                 RedirectAttributes ra) {
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) return "redirect:/login";

        String errorMessage = userService.changePassword(userId, oldPassword, newPassword, confirmPassword);

        if (errorMessage != null) {
            ra.addFlashAttribute("error", errorMessage);
        } else {
            ra.addFlashAttribute("success", "Đổi mật khẩu thành công!");
        }
        return "redirect:/profile";
    }
}
