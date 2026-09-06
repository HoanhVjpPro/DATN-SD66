package com.example.datnhathub.controller;

import com.example.datnhathub.entity.OrderDetail;
import com.example.datnhathub.entity.Orders;
import com.example.datnhathub.entity.ProductDetail;
import com.example.datnhathub.repository.OrderRepository;
import com.example.datnhathub.repository.ProductDetailRepository;
import com.example.datnhathub.service.InvoiceService;
import com.example.datnhathub.service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class OrderController {
    @Autowired
    private OrderRepository ordersRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private InvoiceService invoiceService;

    // ── Danh sách đơn hàng của Customer ──
    @GetMapping("/orders")
    public String orderList(HttpSession session, Model model) {
        Integer customerId = (Integer) session.getAttribute("customerId");
        if (customerId == null) return "redirect:/login";

        List<Orders> orders = ordersRepository.findByCustomerCustomerIdOrderByOrderDateDesc(customerId);
        model.addAttribute("orders", orders);
        return "orders/list"; // templates/orders/list.html
    }

    // ── Chi tiết 1 đơn hàng ──
    @GetMapping("/orders/{id}")
    public String orderDetail(@PathVariable Integer id,
                              HttpSession session,
                              Model model,
                              RedirectAttributes ra) {



        Integer customerId = (Integer) session.getAttribute("customerId");
        if (customerId == null) return "redirect:/login";

        Orders order = ordersRepository.findById(id).orElse(null);

        // Kiểm tra đơn hàng tồn tại và thuộc về customer này
        if (order == null || !order.getCustomer().getCustomerId().equals(customerId)) {
            ra.addFlashAttribute("error", "Không tìm thấy đơn hàng!");
            return "redirect:/orders";
        }

        model.addAttribute("order", order);
        return "orders/detail"; // templates/orders/detail.html
    }

    // ── Customer hủy đơn ──
    @PostMapping("/orders/{id}/cancel")
    public String cancelOrder(@PathVariable Integer id,
                              HttpSession session,
                              RedirectAttributes ra) {

        Integer customerId = (Integer) session.getAttribute("customerId");
        if (customerId == null) return "redirect:/login";

        Orders order = ordersRepository.findById(id).orElse(null);
        if (order == null || !order.getCustomer().getCustomerId().equals(customerId)) {
            ra.addFlashAttribute("error", "Không tìm thấy đơn hàng!");
            return "redirect:/orders";
        }

        if (!"Chờ xác nhận".equals(order.getStatus())) {
            ra.addFlashAttribute("error", "Chỉ hủy được đơn hàng đang ở trạng thái 'Chờ xác nhận'!");
            return "redirect:/orders/" + id;
        }

        orderService.updateOrderStatus(id, "Đã hủy", null); // dùng chung logic hoàn kho + hoàn voucher
        ra.addFlashAttribute("success", "Đã hủy đơn hàng thành công!");
        return "redirect:/orders";
    }

    // ── Customer xác nhận đã nhận hàng ──
    @PostMapping("/orders/{id}/received")
    public String confirmReceived(@PathVariable Integer id,
                                  HttpSession session,
                                  RedirectAttributes ra) {

        Integer customerId = (Integer) session.getAttribute("customerId");
        if (customerId == null) return "redirect:/login";

        Orders order = ordersRepository.findById(id).orElse(null);
        if (order == null || !order.getCustomer().getCustomerId().equals(customerId)) {
            ra.addFlashAttribute("error", "Không tìm thấy đơn hàng!");
            return "redirect:/orders";
        }

        if (!"Đang giao".equals(order.getStatus())) {
            ra.addFlashAttribute("error", "Đơn hàng chưa được giao!");
            return "redirect:/orders/" + id;
        }

        order.setStatus("Hoàn thành");
        ordersRepository.save(order);

        ra.addFlashAttribute("success", "Xác nhận nhận hàng thành công!");
        return "redirect:/orders/" + id;
    }

    // ── Customer tự tải hóa đơn PDF — áp dụng cho MỌI phương thức thanh toán (COD, Chuyển khoản...) ──
    @GetMapping("/orders/{id}/invoice/pdf")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable Integer id, HttpSession session) throws Exception {
        Integer customerId = (Integer) session.getAttribute("customerId");
        if (customerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Orders order = ordersRepository.findById(id).orElse(null);
        if (order == null || !order.getCustomer().getCustomerId().equals(customerId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        byte[] pdfBytes = invoiceService.generateInvoicePdf(order);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=hoadon-donhang-" + order.getOrderCode() + ".pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}