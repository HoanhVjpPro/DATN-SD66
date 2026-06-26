package com.example.datnhathub.controller;

import com.example.datnhathub.entity.Orders;
import com.example.datnhathub.repository.OrderRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PaymentController {

    @Autowired
    private OrderRepository ordersRepository;

    // ── Trang QR ──
    @GetMapping("/payment/qr/{orderId}")
    public String qrPage(@PathVariable Integer orderId,
                         HttpSession session,
                         Model model) {

        Integer customerId = (Integer) session.getAttribute("customerId");
        if (customerId == null) return "redirect:/login";

        Orders order = ordersRepository.findById(orderId).orElse(null);
        if (order == null) return "redirect:/orders";

        // Thông tin ngân hàng — thay bằng thông tin thật
        String bankId      = "MB";
        String accountNo   = "1234567890";
        String accountName = "HATHUB";
        String amount      = order.getTotalAmount().toPlainString();
        String addInfo     = "HatHub" + orderId;

        String qrUrl = "https://img.vietqr.io/image/" + bankId + "-" + accountNo + "-compact2.png"
                + "?amount=" + amount
                + "&addInfo=" + addInfo
                + "&accountName=" + accountName;

        model.addAttribute("order",   order);
        model.addAttribute("qrUrl",   qrUrl);
        model.addAttribute("addInfo", addInfo);
        model.addAttribute("amount",  order.getTotalAmount());
        return "payment/qr"; // templates/payment/qr.html
    }

    // ── Xác nhận đã chuyển khoản ──
    @PostMapping("/payment/confirm/{orderId}")
    public String confirmPayment(@PathVariable Integer orderId,
                                 HttpSession session,
                                 RedirectAttributes ra) {

        Integer customerId = (Integer) session.getAttribute("customerId");
        if (customerId == null) return "redirect:/login";

        Orders order = ordersRepository.findById(orderId).orElse(null);
        if (order == null) return "redirect:/orders";

        // Chuyển status sang "Chờ xác nhận"
        order.setStatus("Chờ xác nhận");
        order.getPayment().setPaymentStatus("Chờ xác nhận");
        ordersRepository.save(order);

        ra.addFlashAttribute("success", "Đã ghi nhận thanh toán! Đơn hàng đang chờ xác nhận.");
        return "redirect:/orders/" + orderId;
    }
}
