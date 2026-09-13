package com.example.datnhathub.controller;

import com.example.datnhathub.dto.SePayWebhookDTO;
import com.example.datnhathub.entity.Orders;
import com.example.datnhathub.entity.Payment;
import com.example.datnhathub.repository.OrderRepository;
import com.example.datnhathub.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/payment")
public class PaymentApiController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @PostMapping("/sepay-webhook")
    public ResponseEntity<?> handleSePayWebhook(@RequestBody SePayWebhookDTO webhookData) {
        if (webhookData == null || webhookData.getContent() == null) {
            return ResponseEntity.badRequest().body("Invalid Payload");
        }

        String content = webhookData.getContent();
        BigDecimal transferAmount = webhookData.getTransferAmount();

        // Tách lấy mã đơn dạng ORDxxxx từ nội dung chuyển khoản
        String orderCode = extractOrderCode(content);

        if (!orderCode.isEmpty()) {
            // Trích xuất phần số từ mã đơn (ví dụ ORD123 -> ID = 123) để tìm theo khóa chính
            Integer orderId = extractOrderId(orderCode);
            Orders order = orderId != null ? orderRepository.findById(orderId).orElse(null) : null;

            if (order != null) {
                // Kiểm tra số tiền nhận được có >= số tiền đơn hàng không
                if (transferAmount != null && transferAmount.compareTo(order.getTotalAmount()) >= 0) {

                    // 1. Cập nhật trạng thái đơn hàng
                    order.setStatus("Đã thanh toán");
                    orderRepository.save(order);

                    // 2. Cập nhật hoặc tạo mới bản ghi trong bảng Payment
                    Payment payment = paymentRepository.findByOrder(order).orElse(null);
                    if (payment == null) {
                        payment = new Payment();
                    }

                    payment.setOrder(order);
                    payment.setPaymentMethod("Chuyển khoản QR");
                    payment.setPaymentStatus("Đã thanh toán");
                    paymentRepository.save(payment);

                    return ResponseEntity.ok(Map.of(
                            "status", "success",
                            "message", "Auto payment confirmed for order " + orderCode
                    ));
                }
            }
        }

        return ResponseEntity.ok(Map.of("status", "ignored", "message", "No matching order found"));
    }

    private String extractOrderCode(String content) {
        Pattern pattern = Pattern.compile("ORD\\d+", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(0).toUpperCase();
        }
        return "";
    }

    private Integer extractOrderId(String orderCode) {
        try {
            String numericPart = orderCode.replaceAll("\\D+", "");
            return Integer.parseInt(numericPart);
        } catch (Exception e) {
            return null;
        }
    }
}