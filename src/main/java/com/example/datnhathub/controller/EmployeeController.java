package com.example.datnhathub.controller;

import com.example.datnhathub.entity.*;
import com.example.datnhathub.repository.OrderRepository;
import com.example.datnhathub.repository.ProductDetailRepository;
import com.example.datnhathub.repository.VoucherRepository;
import com.example.datnhathub.service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/employee")
public class EmployeeController {
    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private ProductDetailRepository productDetailRepository;

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

    @GetMapping("/orders/{id}")
    public String orderDetail(@PathVariable Integer id,
                              Model model,
                              RedirectAttributes ra) {

        Orders order = orderRepository.findById(id).orElse(null);

        if (order == null) {
            ra.addFlashAttribute("error", "Không tìm thấy đơn hàng!");
            return "redirect:/admin/orders";
        }

        model.addAttribute("order", order);
        return "employee/order-detail";
    }

    @GetMapping("/returns")
    public String employeeReturnList(Model model) {
        // Lấy các đơn có ReturnStatus != null
        List<Orders> returns = orderRepository.findByReturnStatusNotNull();
        model.addAttribute("returns", returns);
        return "employee/returns";
    }

    @PostMapping("/returns/{id}/accept")
    public String acceptReturn(@PathVariable Integer id,
                               RedirectAttributes ra) {

        Orders order = orderRepository.findById(id).orElse(null);
        if (order == null) {
            ra.addFlashAttribute("error", "Không tìm thấy đơn hàng!");
            return "redirect:/employee/returns";
        }

        // Cộng lại tồn kho — CHỈ cộng đúng số lượng khách yêu cầu trả ở từng dòng
        // (không hoàn toàn bộ số lượng đã đặt, để hỗ trợ trả hàng một phần)
        if (order.getDetails() != null) {
            for (OrderDetail od : order.getDetails()) {
                if (od.getReturnQuantity() != null && od.getReturnQuantity() > 0) {
                    ProductDetail pd = od.getProductDetail();
                    pd.setStockQuantity(pd.getStockQuantity() + od.getReturnQuantity());
                    productDetailRepository.save(pd);
                }
            }
        }

        // Hoàn lại lượt sử dụng voucher nếu đơn có dùng
        if (order.getVoucher() != null) {
            Voucher v = order.getVoucher();
            v.setQuantity((v.getQuantity() == null ? 0 : v.getQuantity()) + 1);
            voucherRepository.save(v);
        }

        order.setReturnStatus("Đã chấp nhận");
        orderRepository.save(order);

        ra.addFlashAttribute("success", "Đã chấp nhận yêu cầu trả hàng!");
        return "redirect:/employee/returns";
    }

    @PostMapping("/returns/{id}/reject")
    public String rejectReturn(@PathVariable Integer id,
                               RedirectAttributes ra) {

        Orders order = orderRepository.findById(id).orElse(null);
        if (order == null) {
            ra.addFlashAttribute("error", "Không tìm thấy đơn hàng!");
            return "redirect:/employee/returns";
        }

        order.setReturnStatus("Đã từ chối");
        orderRepository.save(order);

        ra.addFlashAttribute("success", "Đã từ chối yêu cầu trả hàng!");
        return "redirect:/employee/returns";
    }
}