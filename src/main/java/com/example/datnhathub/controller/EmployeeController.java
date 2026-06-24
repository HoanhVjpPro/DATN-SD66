package com.example.datnhathub.controller;

import com.example.datnhathub.service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/employee")
public class EmployeeController {
    @Autowired
    private OrderService orderService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("totalOrders", orderService.countAll());
        model.addAttribute("pendingOrders", orderService.countByStatus("Chờ xác nhận"));
        model.addAttribute("shippingOrders", orderService.countByStatus("Đang giao"));
        model.addAttribute("completedOrders", orderService.countByStatus("Hoàn thành"));
        return "employee/dashboard";
    }

    @GetMapping("/orders")
    public String orders(Model model) {
        model.addAttribute("orders", orderService.getAllOrders());
        return "employee/orders";
    }

    @PostMapping("/orders/{id}/status")
    public String updateStatus(@PathVariable Integer id,
                               @RequestParam String status,
                               HttpSession session) {
        orderService.updateOrderStatus(id, status, (Integer) session.getAttribute("userId"));
        return "redirect:/employee/orders";
    }
}
