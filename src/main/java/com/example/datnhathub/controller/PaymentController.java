package com.example.datnhathub.controller;

import com.example.datnhathub.entity.Orders;
import com.example.datnhathub.entity.Payment;
import com.example.datnhathub.entity.Users;
import com.example.datnhathub.repository.OrderRepository;
import com.example.datnhathub.repository.PaymentRepository;
import com.example.datnhathub.service.OrderService;
import com.example.datnhathub.service.VNPayService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class PaymentController {

    @Autowired
    private OrderRepository ordersRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private VNPayService vnPayService;


    // =========================================================
    // XỬ LÝ ĐẶT HÀNG
    // =========================================================

    @PostMapping("/checkout/submit")
    public String submitCheckout(
            @RequestParam(required = false) List<Integer> selectedItems,
            @RequestParam String houseAddress,
            @RequestParam String district,
            @RequestParam String city,
            @RequestParam String paymentMethod,
            @RequestParam(required = false) String voucherCode,
            HttpSession session,
            HttpServletRequest request,
            RedirectAttributes ra
    ) {

        Users user = (Users) session.getAttribute("user");

        if (user == null) {
            return "redirect:/login";
        }

        try {

            // Ghép địa chỉ giao hàng
            String shippingAddress =
                    houseAddress.trim()
                            + ", "
                            + district.trim()
                            + ", "
                            + city.trim();

            // Tạo đơn hàng
            Orders savedOrder = orderService.placeOrder(
                    user.getUserID(),
                    shippingAddress,
                    city,
                    paymentMethod,
                    voucherCode,
                    selectedItems
            );

            // =============================================
            // VNPAY
            // =============================================
            if ("VNPAY".equalsIgnoreCase(paymentMethod)
                    && savedOrder.getTotalAmount() != null
                    && savedOrder.getTotalAmount()
                    .compareTo(BigDecimal.ZERO) > 0) {

                String paymentUrl =
                        vnPayService.createPaymentUrl(
                                savedOrder,
                                request
                        );

                return "redirect:" + paymentUrl;
            }

            // =============================================
            // CHUYỂN KHOẢN QR
            // =============================================
            if ("Chuyển khoản".equalsIgnoreCase(paymentMethod)
                    && savedOrder.getTotalAmount() != null
                    && savedOrder.getTotalAmount()
                    .compareTo(BigDecimal.ZERO) > 0) {

                return "redirect:/payment/qr/"
                        + savedOrder.getOrderId();
            }

            // =============================================
            // COD
            // =============================================
            ra.addFlashAttribute(
                    "success",
                    "Đặt hàng thành công!"
            );

            return "redirect:/orders";

        } catch (Exception e) {

            e.printStackTrace();

            ra.addFlashAttribute(
                    "error",
                    e.getMessage() != null
                            ? e.getMessage()
                            : "Có lỗi xảy ra khi đặt hàng."
            );

            return "redirect:/cart";
        }
    }


    // =========================================================
    // VNPAY RETURN
    // =========================================================

    @GetMapping("/payment/vnpay-payment-return")
    public String vnpayReturn(
            HttpServletRequest request,
            Model model
    ) {

        // =====================================================
        // LẤY PARAMETER TỪ VNPAY
        // =====================================================

        Map<String, String> params =
                new HashMap<>();

        request.getParameterMap()
                .forEach((key, values) -> {

                    if (key.startsWith("vnp_")
                            && !"vnp_SecureHash".equals(key)
                            && !"vnp_SecureHashType".equals(key)
                            && values != null
                            && values.length > 0) {

                        params.put(
                                key,
                                values[0]
                        );
                    }
                });


        String secureHash =
                request.getParameter(
                        "vnp_SecureHash"
                );


        // =====================================================
        // KIỂM TRA CHỮ KÝ
        // =====================================================

        boolean validHash =
                vnPayService.verifyReturn(
                        params,
                        secureHash
                );


        // =====================================================
        // LẤY ORDER ID
        // =====================================================

        String txnRef =
                params.get("vnp_TxnRef");

        Orders order = null;

        try {

            if (txnRef != null) {

                Integer orderId =
                        Integer.valueOf(txnRef);

                order =
                        ordersRepository
                                .findById(orderId)
                                .orElse(null);
            }

        } catch (NumberFormatException e) {

            System.out.println(
                    "VNPay TxnRef không hợp lệ: "
                            + txnRef
            );
        }


        // =====================================================
        // KHÔNG TÌM THẤY ORDER
        // =====================================================

        if (order == null) {

            model.addAttribute(
                    "success",
                    false
            );

            model.addAttribute(
                    "message",
                    "Không tìm thấy đơn hàng."
            );

            return "payment/vnpay-result";
        }


        // =====================================================
        // KIỂM TRA TMN CODE
        // =====================================================

        boolean validTmnCode =
                vnPayService
                        .getTmnCode()
                        .equals(
                                params.get(
                                        "vnp_TmnCode"
                                )
                        );


        // =====================================================
        // KIỂM TRA SỐ TIỀN
        // =====================================================

        boolean validAmount = false;

        try {

            String amountString =
                    params.get("vnp_Amount");

            if (amountString != null) {

                long vnpAmount =
                        Long.parseLong(
                                amountString
                        );

                long orderAmount =
                        order.getTotalAmount()
                                .multiply(
                                        BigDecimal.valueOf(100)
                                )
                                .longValueExact();

                validAmount =
                        vnpAmount == orderAmount;
            }

        } catch (Exception e) {

            validAmount = false;
        }


        // =====================================================
        // KIỂM TRA KẾT QUẢ VNPAY
        // =====================================================

        String responseCode =
                params.get(
                        "vnp_ResponseCode"
                );

        String transactionStatus =
                params.get(
                        "vnp_TransactionStatus"
                );


        boolean paymentSuccess =
                validHash
                        && validTmnCode
                        && validAmount
                        && "00".equals(
                        responseCode
                )
                        && "00".equals(
                        transactionStatus
                );


        // =====================================================
        // THANH TOÁN THÀNH CÔNG
        // =====================================================

        if (paymentSuccess) {

            Payment payment =
                    paymentRepository
                            .findByOrder(order)
                            .orElse(
                                    order.getPayment()
                            );


            if (payment != null) {

                /*
                 * Không cập nhật lại nếu đơn
                 * đã được thanh toán trước đó.
                 */
                if (!"Đã thanh toán".equals(
                        payment.getPaymentStatus()
                )) {

                    payment.setPaymentMethod(
                            "VNPAY"
                    );

                    payment.setPaymentStatus(
                            "Đã thanh toán"
                    );

                    paymentRepository.save(
                            payment
                    );


                    order.setStatus(
                            "Chờ xác nhận"
                    );

                    ordersRepository.save(
                            order
                    );
                }
            }


            model.addAttribute(
                    "success",
                    true
            );

            model.addAttribute(
                    "message",
                    "Thanh toán VNPay thành công!"
            );

        } else {

            model.addAttribute(
                    "success",
                    false
            );

            model.addAttribute(
                    "message",
                    "Thanh toán VNPay không thành công."
            );
        }


        // =====================================================
        // DỮ LIỆU HIỂN THỊ
        // =====================================================

        model.addAttribute(
                "order",
                order
        );

        model.addAttribute(
                "responseCode",
                responseCode
        );

        model.addAttribute(
                "transactionNo",
                params.get(
                        "vnp_TransactionNo"
                )
        );


        return "payment/vnpay-result";
    }


    // =========================================================
    // TRANG QR CHUYỂN KHOẢN CŨ
    // =========================================================

    @GetMapping("/payment/qr/{orderId}")
    public String qrPage(
            @PathVariable Integer orderId,
            HttpSession session,
            Model model
    ) {

        Integer customerId =
                (Integer) session.getAttribute(
                        "customerId"
                );

        if (customerId == null) {
            return "redirect:/login";
        }


        Orders order =
                ordersRepository
                        .findById(orderId)
                        .orElse(null);

        if (order == null) {
            return "redirect:/orders";
        }


        String bankId =
                "MBBank";

        String accountNo =
                "0984085074";

        String accountName =
                "HATHUB";

        String amount =
                order.getTotalAmount()
                        .toPlainString();

        String addInfo =
                order.getOrderCode();


        String qrUrl =
                "https://img.vietqr.io/image/"
                        + bankId
                        + "-"
                        + accountNo
                        + "-compact.png"
                        + "?amount="
                        + amount
                        + "&addInfo="
                        + addInfo
                        + "&accountName="
                        + accountName;


        model.addAttribute(
                "order",
                order
        );

        model.addAttribute(
                "qrUrl",
                qrUrl
        );

        model.addAttribute(
                "addInfo",
                addInfo
        );

        model.addAttribute(
                "amount",
                order.getTotalAmount()
        );


        return "payment/qr";
    }


    // =========================================================
    // XÁC NHẬN CHUYỂN KHOẢN QR CŨ
    // =========================================================

    @PostMapping("/payment/confirm/{orderId}")
    public String confirmPayment(
            @PathVariable Integer orderId,
            HttpSession session,
            RedirectAttributes ra
    ) {

        Integer customerId =
                (Integer) session.getAttribute(
                        "customerId"
                );

        if (customerId == null) {
            return "redirect:/login";
        }


        Orders order =
                ordersRepository
                        .findById(orderId)
                        .orElse(null);

        if (order == null) {
            return "redirect:/orders";
        }


        order.setStatus(
                "Chờ xác nhận"
        );


        if (order.getPayment() != null) {

            order.getPayment()
                    .setPaymentStatus(
                            "Chờ xác nhận"
                    );
        }


        ordersRepository.save(
                order
        );


        ra.addFlashAttribute(
                "success",
                "Đã ghi nhận thanh toán! Đơn hàng đang chờ xác nhận."
        );


        return "redirect:/orders/"
                + orderId;
    }
}