package com.example.datnhathub.controller;

import com.example.datnhathub.entity.OrderDetail;
import com.example.datnhathub.entity.Orders;
import com.example.datnhathub.entity.ProductDetail;
import com.example.datnhathub.entity.Voucher;
import com.example.datnhathub.repository.OrderRepository;
import com.example.datnhathub.repository.ProductDetailRepository;
import com.example.datnhathub.repository.VoucherRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Controller
public class ReturnRequestController {

    @Autowired
    private OrderRepository ordersRepository;

    @Autowired
    private ProductDetailRepository productDetailRepository;

    @Autowired
    private VoucherRepository voucherRepository;

    // ── Customer xem trang trả hàng ──
    @GetMapping("/orders/{id}/return")
    public String returnPage(@PathVariable Integer id,
                             HttpSession session,
                             Model model) {

        Integer customerId = (Integer) session.getAttribute("customerId");
        if (customerId == null) return "redirect:/login";

        Orders order = ordersRepository.findById(id).orElse(null);
        if (order == null || !order.getCustomer().getCustomerId().equals(customerId))
            return "redirect:/orders";

        if (!"Hoàn thành".equals(order.getStatus()))
            return "redirect:/orders/" + id;

        model.addAttribute("order", order);
        return "orders/return";
    }

    // ── Customer gửi yêu cầu trả hàng ──
    @PostMapping("/orders/{id}/return")
    public String submitReturn(@PathVariable Integer id,
                               @RequestParam String reason,
                               HttpSession session,
                               RedirectAttributes ra) {

        Integer customerId = (Integer) session.getAttribute("customerId");
        if (customerId == null) return "redirect:/login";

        Orders order = ordersRepository.findById(id).orElse(null);
        if (order == null || !order.getCustomer().getCustomerId().equals(customerId))
            return "redirect:/orders";

        if (!"Hoàn thành".equals(order.getStatus())) {
            ra.addFlashAttribute("error", "Chỉ trả hàng khi đơn đã hoàn thành!");
            return "redirect:/orders/" + id;
        }

        // Đã có yêu cầu rồi
        if (order.getReturnStatus() != null) {
            ra.addFlashAttribute("error", "Đơn hàng này đã có yêu cầu trả hàng!");
            return "redirect:/orders/" + id + "/return";
        }

        order.setReturnStatus("Chờ xác nhận");
        order.setReturnReason(reason);
        order.setReturnDate(LocalDateTime.now());
        ordersRepository.save(order);

        ra.addFlashAttribute("success", "Đã gửi yêu cầu trả hàng!");
        return "redirect:/orders/" + id + "/return";
    }

    // ── Admin xem danh sách yêu cầu trả hàng ──
    @GetMapping("/admin/returns")
    public String adminReturnList(Model model) {
        // Lấy các đơn có ReturnStatus != null
        List<Orders> returns = ordersRepository.findByReturnStatusNotNull();
        model.addAttribute("returns", returns);
        return "admin/returns";
    }

    // ── Admin chấp nhận ──
    @PostMapping("/admin/returns/{id}/accept")
    public String acceptReturn(@PathVariable Integer id,
                               RedirectAttributes ra) {

        Orders order = ordersRepository.findById(id).orElse(null);
        if (order == null) {
            ra.addFlashAttribute("error", "Không tìm thấy đơn hàng!");
            return "redirect:/admin/returns";
        }

        // Cộng lại tồn kho
        if (order.getDetails() != null) {
            for (OrderDetail od : order.getDetails()) {
                ProductDetail pd = od.getProductDetail();
                pd.setStockQuantity(pd.getStockQuantity() + od.getQuantity());
                productDetailRepository.save(pd);
            }
        }

        // Hoàn lại lượt sử dụng voucher nếu đơn có dùng
        if (order.getVoucher() != null) {
            Voucher v = order.getVoucher();
            v.setQuantity((v.getQuantity() == null ? 0 : v.getQuantity()) + 1);
            voucherRepository.save(v);
        }

        order.setReturnStatus("Đã chấp nhận");
        ordersRepository.save(order);

        ra.addFlashAttribute("success", "Đã chấp nhận yêu cầu trả hàng!");
        return "redirect:/admin/returns";
    }

    // ── Admin từ chối ──
    @PostMapping("/admin/returns/{id}/reject")
    public String rejectReturn(@PathVariable Integer id,
                               RedirectAttributes ra) {

        Orders order = ordersRepository.findById(id).orElse(null);
        if (order == null) {
            ra.addFlashAttribute("error", "Không tìm thấy đơn hàng!");
            return "redirect:/admin/returns";
        }

        order.setReturnStatus("Đã từ chối");
        ordersRepository.save(order);

        ra.addFlashAttribute("success", "Đã từ chối yêu cầu trả hàng!");
        return "redirect:/admin/returns";
    }
}
