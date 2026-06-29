package com.example.datnhathub.controller;

import com.example.datnhathub.entity.Users;
import com.example.datnhathub.entity.Voucher;
import com.example.datnhathub.service.*;
import jakarta.servlet.http.HttpSession;
import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/admin")
public class AdminController {
    @Autowired
    private ProductService productService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private VoucherService voucherService;
    @Autowired
    private AdminService adminService;

    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session) {
//        Users user = (Users) session.getAttribute("user");
//        if (user == null) {
//            return "redirect:/login";
//        }
//        if (!"ADMIN".equals(user.getRole())) {
//            return "access-denied";
//        }
        model.addAttribute("totalProducts", productService.getAllProducts().size());
        model.addAttribute("totalCategories", categoryService.getAll().size());
        model.addAttribute("totalOrders", orderService.countAll());
        model.addAttribute("pendingOrders", orderService.countByStatus("Chờ xác nhận"));
        model.addAttribute("totalUsers", adminService.getAllUsers().size());
        return "admin/dashboard";
    }

    // ── VOUCHERS ──
    @GetMapping("/vouchers")
    public String vouchers(Model model) {
        model.addAttribute("vouchers", voucherService.getAll());
        model.addAttribute("voucher", new Voucher());
        return "admin/vouchers";
    }

    @PostMapping("/vouchers")
    public String saveVoucher(@RequestParam(required = false) Integer voucherId,
                              @RequestParam String code,
                              @RequestParam BigDecimal discountAmount,
                              @RequestParam Integer quantity) {
        Voucher v = voucherId != null ? voucherService.getById(voucherId) : new Voucher();
        if (v == null) v = new Voucher();
        v.setCode(code);
        v.setDiscountAmount(discountAmount);
        v.setQuantity(quantity);
        voucherService.save(v);
        return "redirect:/admin/vouchers";
    }

    @PostMapping("/vouchers/delete/{id}")
    public String deleteVoucher(@PathVariable Integer id) {
        voucherService.delete(id);
        return "redirect:/admin/vouchers";
    }

    // ── USERS ──
    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("users", adminService.getAllUsers());
        return "admin/users";
    }

    @PostMapping("/users/toggle/{id}")
    public String toggleUser(@PathVariable Integer id) {
        adminService.toggleUserStatus(id);
        return "redirect:/admin/users";
    }

    @PostMapping("/users/employee")
    public String createEmployee(@RequestParam String username,
                                 @RequestParam String password,
                                 @RequestParam String email,
                                 @RequestParam(required = false) String phone,
                                 RedirectAttributes ra) {
        try {
            adminService.createEmployee(username, password, email, phone);
            ra.addFlashAttribute("success", "Tạo nhân viên thành công");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    // ── orders ──
    @GetMapping("/orders")
    public String orders(Model model) {
        model.addAttribute("orders", orderService.getAllOrders());
        return "admin/orders";
    }

    @PostMapping("/orders/{id}/status")
    public String updateOrderStatus(@PathVariable Integer id, @RequestParam String status) {
        orderService.updateOrderStatus(id, status, null);
        return "redirect:/admin/orders";
    }
}
