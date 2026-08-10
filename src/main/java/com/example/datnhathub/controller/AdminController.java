package com.example.datnhathub.controller;

import com.example.datnhathub.entity.*;
import com.example.datnhathub.repository.OrderRepository;
import com.example.datnhathub.repository.ReviewRepository;
import com.example.datnhathub.repository.UserRepository;
import com.example.datnhathub.service.*;
import jakarta.servlet.http.HttpSession;
import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.example.datnhathub.entity.Users;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.math.BigDecimal;
import java.util.List;

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
    private OrderRepository orderRepository;

    @Autowired
    private VoucherService voucherService;

    @Autowired
    private AdminService adminService;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private ReviewRepository reviewRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session) {
        Users user = (Users) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        if (!user.getRole().getRoleId().equals(1)) {
            return "access-denied";
        }
        model.addAttribute("totalProducts", productService.getAllProducts().size());
        model.addAttribute("totalCategories", categoryService.getAll().size());
        model.addAttribute("totalOrders", orderService.countAll());
        model.addAttribute("pendingOrders", orderService.countByStatus("Chờ xác nhận"));
        model.addAttribute("totalUsers", adminService.getAllUsers().size());

        var monthlyRevenue = orderService.getMonthlyRevenue();
        model.addAttribute("revenueLabels",
                monthlyRevenue.stream().map(OrderRepository.MonthlyRevenueProjection::getMonth).toList());
        model.addAttribute("revenueData",
                monthlyRevenue.stream().map(OrderRepository.MonthlyRevenueProjection::getRevenue).toList());

        var dailyRevenue = orderService.getDailyRevenue();
        model.addAttribute("dailyLabels",
                dailyRevenue.stream().map(OrderRepository.DailyRevenueProjection::getDay).toList());
        model.addAttribute("dailyData",
                dailyRevenue.stream().map(OrderRepository.DailyRevenueProjection::getRevenue).toList());

        var weeklyRevenue = orderService.getWeeklyRevenue();
        model.addAttribute("weeklyLabels",
                weeklyRevenue.stream().map(OrderRepository.WeeklyRevenueProjection::getWeek).toList());
        model.addAttribute("weeklyData",
                weeklyRevenue.stream().map(OrderRepository.WeeklyRevenueProjection::getRevenue).toList());

        model.addAttribute("topProducts", orderService.getTopSellingProducts());

        return "admin/dashboard";
    }

    // ── VOUCHERS ──
    @GetMapping("/vouchers")
    public String vouchers(Model model, HttpSession session) {
        Users user = (Users) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        if (!user.getRole().getRoleId().equals(1)) {
            return "access-denied";
        }
        model.addAttribute("vouchers", voucherService.getAll());
        model.addAttribute("voucher", new Voucher());
        return "admin/vouchers";
    }

    @PostMapping("/vouchers")
    public String saveVoucher(@RequestParam(required = false) Integer voucherId,
                              @RequestParam String code,
                              @RequestParam String discountType,
                              @RequestParam(required = false) BigDecimal discountAmount,
                              @RequestParam(required = false) BigDecimal discountPercent,
                              @RequestParam(required = false) BigDecimal maxDiscountAmount,
                              @RequestParam(required = false) LocalDate expiryDate,
                              @RequestParam Integer quantity,
                              RedirectAttributes ra) {
        Voucher v = voucherId != null ? voucherService.getById(voucherId) : new Voucher();
        if (v == null) v = new Voucher();
        v.setCode(code);
        v.setDiscountType(discountType);
        v.setDiscountAmount(discountAmount);
        v.setDiscountPercent(discountPercent);
        v.setMaxDiscountAmount(maxDiscountAmount);
        v.setExpiryDate(expiryDate);
        v.setQuantity(quantity);

        try {
            voucherService.save(v);
            ra.addFlashAttribute("success", "Tạo voucher thành công");
        }catch (DataIntegrityViolationException e){
            ra.addFlashAttribute("error", "Mã Voucher đã tồn tại");
        }

        return "redirect:/admin/vouchers";
    }

    @PostMapping("/vouchers/delete/{id}")
    public String deleteVoucher(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            voucherService.delete(id);
            ra.addFlashAttribute("success", "Xóa voucher thành công");
        } catch (DataIntegrityViolationException e) {
            ra.addFlashAttribute("error", "Voucher đang được xử dụng cho đơn hàng chưa được xử lý!");
        }
        return "redirect:/admin/vouchers";
    }

    // ── Trang chọn người nhận voucher ──
    @GetMapping("/vouchers/{id}/send")
    public String showSendVoucherPage(@PathVariable Integer id, Model model, RedirectAttributes ra) {
        Voucher voucher = voucherService.getById(id);
        if (voucher == null) {
            ra.addFlashAttribute("error", "Không tìm thấy voucher!");
            return "redirect:/admin/vouchers";
        }
        model.addAttribute("voucher", voucher);
        model.addAttribute("users", adminService.getAllUsers());
        return "admin/voucher-send";
    }

    // ── Gửi cho các tài khoản được chọn ──
    @PostMapping("/vouchers/{id}/send")
    public String sendVoucherToSelected(@PathVariable Integer id,
                                        @RequestParam(required = false) List<Integer> userIds,
                                        RedirectAttributes ra) {
        Voucher voucher = voucherService.getById(id);
        if (voucher == null) {
            ra.addFlashAttribute("error", "Không tìm thấy voucher!");
            return "redirect:/admin/vouchers";
        }
        if (userIds == null || userIds.isEmpty()) {
            ra.addFlashAttribute("error", "Vui lòng chọn ít nhất 1 tài khoản!");
            return "redirect:/admin/vouchers/" + id + "/send";
        }

        Set<Integer> idSet = new HashSet<>(userIds);
        int sent = 0;
        for (Users user : adminService.getAllUsers()) {
            if (idSet.contains(user.getUserID()) && user.getEmail() != null && !user.getEmail().isBlank()) {
                sendVoucherEmail(user, voucher);
                sent++;
            }
        }
        ra.addFlashAttribute("success", "Đã gửi voucher cho " + sent + " tài khoản!");
        return "redirect:/admin/vouchers";
    }

    // ── Gửi cho toàn bộ người dùng ──
    @PostMapping("/vouchers/{id}/send-all")
    public String sendVoucherToAll(@PathVariable Integer id, RedirectAttributes ra) {
        Voucher voucher = voucherService.getById(id);
        if (voucher == null) {
            ra.addFlashAttribute("error", "Không tìm thấy voucher!");
            return "redirect:/admin/vouchers";
        }

        int sent = 0;
        for (Users user : adminService.getAllUsers()) {
            if (user.getEmail() != null && !user.getEmail().isBlank()) {
                sendVoucherEmail(user, voucher);
                sent++;
            }
        }
        ra.addFlashAttribute("success", "Đã gửi voucher cho toàn bộ " + sent + " người dùng!");
        return "redirect:/admin/vouchers";
    }

    private void sendVoucherEmail(Users user, Voucher voucher) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("HatHub – Mã giảm giá dành cho bạn!");
        message.setText(buildVoucherEmailBody(user, voucher));
        mailSender.send(message);
    }

    private String buildVoucherEmailBody(Users user, Voucher voucher) {
        DecimalFormat df = new DecimalFormat("#,###");
        String discountText;
        if (voucher.isFreeShip()) {
            discountText = "miễn phí vận chuyển";
        } else if ("PERCENT".equals(voucher.getDiscountType())) {
            discountText = "giảm " + voucher.getDiscountPercent() + "%"
                    + (voucher.getMaxDiscountAmount() != null
                    ? " (tối đa " + df.format(voucher.getMaxDiscountAmount()) + " đ)" : "");
        } else {
            discountText = "giảm " + df.format(voucher.getDiscountAmount()) + " đ";
        }

        String expiryText = voucher.getExpiryDate() != null
                ? "\nHạn sử dụng: " + voucher.getExpiryDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                : "";

        return "Xin chào " + user.getUsername() + ",\n\n" +
                "HatHub gửi tặng bạn mã voucher: " + voucher.getCode() + "\n" +
                "Ưu đãi: " + discountText + expiryText + "\n\n" +
                "Hãy nhập mã này khi thanh toán để nhận ưu đãi nhé!\n\n" +
                "Trân trọng,\nHatHub Team";
    }

    // ── USERS ──
    @GetMapping("/users")
    public String users(Model model, HttpSession session,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "5") int size) {
        Users user = (Users) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        if (!user.getRole().getRoleId().equals(1)) {
            return "access-denied";
        }

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
    public String orders(Model model, HttpSession session) {
        Users user = (Users) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        if (!user.getRole().getRoleId().equals(1)) {
            return "access-denied";
        }
        model.addAttribute("orders", orderService.getAllOrders());
        return "admin/orders";
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
        return "admin/order-detail";
    }

    @PostMapping("/orders/{id}/status")
    public String updateOrderStatus(@PathVariable Integer id, @RequestParam String status, RedirectAttributes ra) {
        try {
            orderService.updateOrderStatus(id, status, null);
            ra.addFlashAttribute("success", "Đã cập nhật trạng thái đơn hàng!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }

    @GetMapping("/products/sales")
    public String allProductSales(Model model, HttpSession session) {
        Users user = (Users) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        if (!user.getRole().getRoleId().equals(1)) {
            return "access-denied";
        }
        model.addAttribute("productSales", orderService.getAllProductSales());
        return "admin/all-doanh-so";
    }

    @PostMapping("/vouchers/{id}/edit-quantity-expiry")
    public String editVoucherQuantityExpiry(@PathVariable Integer id,
                                            @RequestParam Integer quantity,
                                            @RequestParam(required = false) LocalDate expiryDate,
                                            RedirectAttributes ra) {
        try {
            voucherService.updateQuantityAndExpiry(id, quantity, expiryDate);
            ra.addFlashAttribute("success", "Đã cập nhật số lượng/hạn dùng voucher!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/vouchers";
    }

    @PostMapping("/users/shipper")
    public String createShipper(@RequestParam String username,
                                @RequestParam String password,
                                @RequestParam String email,
                                @RequestParam(required = false) String phone,
                                RedirectAttributes ra) {
        try {
            adminService.createShipper(username, password, email, phone);
            ra.addFlashAttribute("success", "Tạo shipper thành công");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/orders/{id}/incident/refund")
    public String resolveIncidentRefund(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            orderService.resolveIncidentByRefund(id);
            ra.addFlashAttribute("success", "Đã chuyển đơn sang CSKH xử lý hoàn tiền!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }

    @PostMapping("/orders/{id}/incident/reship")
    public String resolveIncidentReship(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            orderService.resolveIncidentByReship(id);
            ra.addFlashAttribute("success", "Đã chuyển đơn về trạng thái giao lại!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }

    @GetMapping("/reviews")
    public String adminReviewList(Model model, HttpSession session) {
        Users user = (Users) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        if (!user.getRole().getRoleId().equals(1)) {
            return "access-denied";
        }

        model.addAttribute("reviews", reviewRepository.findAll());
        return "admin/reviews"; // Trỏ tới file template: templates/admin/reviews.html
    }

    @PostMapping("/reviews/{id}/reply")
    public String replyReview(@PathVariable Integer id,
                              @RequestParam("adminReply") String adminReply,
                              RedirectAttributes ra) {
        Reviews review = reviewRepository.findById(id).orElse(null);
        if (review == null) {
            ra.addFlashAttribute("error", "Không tìm thấy đánh giá!");
            return "redirect:/admin/reviews";
        }

        review.setAdminReply(adminReply);
        reviewRepository.save(review);

        ra.addFlashAttribute("success", "Đã gửi phản hồi đánh giá thành công!");
        return "redirect:/admin/reviews";
    }
}
