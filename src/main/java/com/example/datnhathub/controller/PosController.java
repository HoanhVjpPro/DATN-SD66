package com.example.datnhathub.controller;

import com.example.datnhathub.dto.*;
import com.example.datnhathub.entity.PosCustomer;
import com.example.datnhathub.entity.PosOrder;
import com.example.datnhathub.entity.Users;
import com.example.datnhathub.service.PosService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/pos")
public class PosController {

    @Autowired
    private PosService posService;

    // Chỉ ADMIN hoặc EMPLOYEE được vào bán hàng tại quầy
    private boolean isStaff(HttpSession session) {
        String roleName = (String) session.getAttribute("roleName");
        return "ADMIN".equals(roleName) || "EMPLOYEE".equals(roleName);
    }

    // GET /pos — Trang bán hàng tại quầy
    @GetMapping
    public String posPage(HttpSession session, Model model) {
        Users user = (Users) session.getAttribute("user");
        if (user == null) return "redirect:/login";
        if (!isStaff(session)) return "access-denied";

        model.addAttribute("variants", posService.getAllVariantsForPos());
        return "employee/pos";
    }

    // GET /pos/history — Lịch sử đơn POS
    @GetMapping("/history")
    public String posHistory(HttpSession session, Model model) {
        Users user = (Users) session.getAttribute("user");
        if (user == null) return "redirect:/login";
        if (!isStaff(session)) return "access-denied";

        List<PosOrder> posOrders = posService.getAllPosOrders();
        model.addAttribute("posOrders", posOrders);
        model.addAttribute("totalRevenue", posService.getTotalRevenue(posOrders));
        return "employee/pos-history";
    }

    // POST /pos/orders/{id}/shipping-status — Cập nhật trạng thái giao hàng tận nơi
    @PostMapping("/orders/{id}/shipping-status")
    public String updateShippingStatus(@PathVariable Integer id,
                                       @RequestParam String status,
                                       HttpSession session,
                                       RedirectAttributes ra) {
        Users user = (Users) session.getAttribute("user");
        if (user == null) return "redirect:/login";
        if (!isStaff(session)) return "access-denied";

        try {
            posService.updateShippingStatus(id, status);
            ra.addFlashAttribute("success", "Đã cập nhật trạng thái giao hàng!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/pos/history";
    }

    // GET /pos/api/customer?phone=xxx — Tra cứu khách hàng theo SĐT (AJAX)
    @GetMapping("/api/customer")
    @ResponseBody
    public ResponseEntity<?> lookupCustomer(@RequestParam String phone) {
        Optional<PosCustomer> customer = posService.findByPhone(phone);
        if (customer.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toDto(customer.get()));
    }

    // POST /pos/customer — Tạo khách hàng POS mới (AJAX)
    @PostMapping("/customer")
    @ResponseBody
    public ResponseEntity<?> createCustomer(@RequestBody PosNewCustomerRequest req) {
        try {
            PosCustomer customer = posService.createCustomer(
                    req.getPhone(), req.getFullName(), req.getAddress(), req.getEmail());
            return ResponseEntity.ok(toDto(customer));
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // POST /pos/checkout — Thanh toán đơn tại quầy (AJAX)
    @PostMapping("/checkout")
    @ResponseBody
    public ResponseEntity<?> checkout(@RequestBody PosCheckoutRequest req, HttpSession session) {
        Integer employeeUserId = (Integer) session.getAttribute("userId");
        if (employeeUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại."));
        }

        try {
            PosOrder order = posService.checkout(req, employeeUserId);
            return ResponseEntity.ok(new PosCheckoutResponse(order.getPosOrderId(), order.getOrderCode()));
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    private PosCustomerDto toDto(PosCustomer c) {
        return new PosCustomerDto(c.getPosCustomerId(), c.getFullName(), c.getPhone(), c.getEmail(), c.getAddress());
    }
}