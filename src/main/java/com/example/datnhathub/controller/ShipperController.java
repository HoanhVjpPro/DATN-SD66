package com.example.datnhathub.controller;

import com.example.datnhathub.entity.Orders;
import com.example.datnhathub.service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/shipper")
public class ShipperController {

    @Autowired
    private OrderService orderService;

    private Integer requireShipperUserId(HttpSession session) {
        return (Integer) session.getAttribute("userId");
    }

    // GET /shipper/dashboard — đơn đang phụ trách + pool đơn chưa ai nhận
    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        Integer userId = requireShipperUserId(session);
        if (userId == null) return "redirect:/login";

        List<Orders> myOrders = orderService.getOrdersForShipper(userId);
        List<Orders> availableOrders = orderService.getUnclaimedOrders();

        model.addAttribute("myOrders", myOrders);
        model.addAttribute("availableOrders", availableOrders);
        return "shipper/dashboard";
    }

    // POST /shipper/orders/{id}/claim — nhận đơn
    @PostMapping("/orders/{id}/claim")
    public String claimOrder(@PathVariable Integer id, HttpSession session, RedirectAttributes ra) {
        Integer userId = requireShipperUserId(session);
        if (userId == null) return "redirect:/login";

        try {
            orderService.claimOrder(id, userId);
            ra.addFlashAttribute("success", "Đã nhận đơn #" + id + "!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/shipper/dashboard";
    }

    // POST /shipper/orders/{id}/confirm-delivered — xác nhận đã giao thành công
    @PostMapping("/orders/{id}/confirm-delivered")
    public String confirmDelivered(@PathVariable Integer id, HttpSession session, RedirectAttributes ra) {
        Integer userId = requireShipperUserId(session);
        if (userId == null) return "redirect:/login";

        try {
            orderService.confirmDeliveredByShipper(id, userId);
            ra.addFlashAttribute("success", "Đã xác nhận giao hàng thành công cho đơn #" + id + "!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/shipper/dashboard";
    }

    // POST /shipper/orders/{id}/report-incident — báo sự cố (mất hàng/giao thất bại)
    @PostMapping("/orders/{id}/report-incident")
    public String reportIncident(@PathVariable Integer id,
                                 @RequestParam String reason,
                                 HttpSession session,
                                 RedirectAttributes ra) {
        Integer userId = requireShipperUserId(session);
        if (userId == null) return "redirect:/login";

        try {
            orderService.reportIncident(id, userId, reason);
            ra.addFlashAttribute("success", "Đã báo sự cố cho đơn #" + id + ". Admin sẽ xử lý sớm nhất.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/shipper/dashboard";
    }
}