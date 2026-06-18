package com.example.datnhathub.controller;

import com.example.datnhathub.entity.Users;
import com.example.datnhathub.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ForgotPasswordController {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JavaMailSender mailSender;

    // ════════════════════════════════════════
    // BƯỚC 1 — Hiện form nhập email
    // ════════════════════════════════════════
    @GetMapping("/forgot-password")
    public String showForgotPassword() {
        return "forgot-password"; // templates/forgot-password.html
    }

    @PostMapping("/forgot-password")
    public String ForgotPassword(@RequestParam("email") String email,
                                       HttpSession session,
                                       RedirectAttributes ra) {

        Users user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            ra.addFlashAttribute("error", "Email không tồn tại trong hệ thống!");
            return "redirect:/forgot-password";
        }

        // Tạo OTP 6 chữ số
        int otp = (int) (Math.random() * 900000) + 100000;

        // Lưu OTP và email vào session
        session.setAttribute("otp",        String.valueOf(otp));
        session.setAttribute("resetEmail", email);

        // Gửi email
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("HatHub – Mã xác nhận đặt lại mật khẩu");
        message.setText(
                "Xin chào " + user.getUsername() + ",\n\n" +
                        "Mã OTP của bạn là: " + otp + "\n\n" +
                        "Trân trọng,\nHatHub Team"
        );
        mailSender.send(message);

        ra.addFlashAttribute("success", "Mã OTP đã được gửi đến " + email);
        return "redirect:/verify-otp";
    }

    // ════════════════════════════════════════
    // BƯỚC 2 — Hiện form nhập OTP + mật khẩu mới
    // ════════════════════════════════════════
    @GetMapping("/verify-otp")
    public String showVerifyOtp(HttpSession session, RedirectAttributes ra) {

        // Nếu chưa có session OTP → quay lại bước 1
        if (session.getAttribute("otp") == null) {
            ra.addFlashAttribute("error", "Vui lòng nhập email trước!");
            return "redirect:/forgot-password";
        }
        return "verify-otp"; // templates/verify-otp.html
    }

    @PostMapping("/verify-otp")
    public String handleVerifyOtp(@RequestParam("otp")             String otp,
                                  @RequestParam("newPassword")     String newPassword,
                                  @RequestParam("confirmPassword") String confirmPassword,
                                  HttpSession session,
                                  RedirectAttributes ra) {

        String sessionOtp   = (String) session.getAttribute("otp");
        String resetEmail   = (String) session.getAttribute("resetEmail");

        // Kiểm tra OTP
        if (sessionOtp == null || !otp.equals(sessionOtp)) {
            ra.addFlashAttribute("error", "Mã OTP không đúng!");
            return "redirect:/verify-otp";
        }

        // Kiểm tra mật khẩu khớp
        if (!newPassword.equals(confirmPassword)) {
            ra.addFlashAttribute("error", "Mật khẩu xác nhận không khớp!");
            return "redirect:/verify-otp";
        }

        // Tìm user và cập nhật mật khẩu
        Users user = userRepository.findByEmail(resetEmail).orElse(null);
        if (user == null) {
            ra.addFlashAttribute("error", "Có lỗi xảy ra, vui lòng thử lại!");
            return "redirect:/forgot-password";
        }

        user.setPassword(newPassword); // plain text theo yêu cầu
        userRepository.save(user);

        // Xóa session OTP sau khi dùng
        session.removeAttribute("otp");
        session.removeAttribute("resetEmail");

        ra.addFlashAttribute("success", "Đổi mật khẩu thành công! Vui lòng đăng nhập.");
        return "redirect:/login";
    }
}
