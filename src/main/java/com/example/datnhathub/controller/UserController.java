package com.example.datnhathub.controller;

import com.example.datnhathub.dto.RegisterDto;
import com.example.datnhathub.entity.Users;
import com.example.datnhathub.repository.UserRepository;
import com.example.datnhathub.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UserController {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    private boolean isLoggedIn(HttpSession session) {
        return session.getAttribute("loggedInUser") != null;
    }

    @GetMapping("/")
    public String index() {
        return "main-menu";
    }

    @GetMapping("/login")
    public String showLoginPage(HttpSession session, Model model,
                                @RequestParam(required = false) String error,
                                @RequestParam(required = false) String success) {

        // Nếu đã đăng nhập → redirect theo role
        if (isLoggedIn(session)) {
            return redirectByRole(session);
        }

        if (error != null)   model.addAttribute("error",   "Tên đăng nhập hoặc mật khẩu không đúng!");
        if (success != null) model.addAttribute("success", "Đăng ký thành công! Vui lòng đăng nhập.");

        return "login"; // → templates/login.html
    }

    @PostMapping("/login")
    public String Login(@RequestParam String username,
                              @RequestParam String password,
                              HttpSession session,
                              RedirectAttributes redirectAttrs) {

        Users user = userService.login(username, password);

        if (user == null) {
            // Sai thông tin hoặc bị khóa
            redirectAttrs.addAttribute("error", "true");
            return "redirect:/login";
        }

        // Lưu thông tin vào Session
        session.setAttribute("loggedInUser", user);
        session.setAttribute("userId",       user.getUserID());
        session.setAttribute("username",     user.getUsername());
        session.setAttribute("roleName",     user.getRole().getRoleName());

        session.setAttribute("user", user);
        // Redirect theo role
        return redirectByRole(session);
    }

    @GetMapping("/register")
    public String showRegisterPage(HttpSession session, Model model) {

        if (isLoggedIn(session)) {
            return redirectByRole(session);
        }

        model.addAttribute("registerDTO", new RegisterDto());
        return "register"; // → templates/register.html
    }

    @PostMapping("/register")
    public String Register(@ModelAttribute RegisterDto dto,
                                 Model model,
                                 RedirectAttributes redirectAttrs) {

        // Gọi service — trả về null nếu thành công, trả về chuỗi lỗi nếu thất bại
        String errorMsg = userService.register(dto);

        if (errorMsg != null) {
            // Có lỗi → quay lại form, giữ dữ liệu đã nhập
            model.addAttribute("error",       errorMsg);
            model.addAttribute("registerDTO", dto);
            return "register";
        }

        // Thành công → sang trang login kèm thông báo
        redirectAttrs.addAttribute("success", "true");
        return "redirect:/login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    private String redirectByRole(HttpSession session) {
        String role = (String) session.getAttribute("roleName");
        if (role == null) return "redirect:/login";

        return switch (role) {
            case "ADMIN"    -> "redirect:/admin/dashboard";
            case "EMPLOYEE" -> "redirect:/employee/dashboard";
            default         -> "redirect:/";           // CUSTOMER → trang chủ
        };
    }

}
