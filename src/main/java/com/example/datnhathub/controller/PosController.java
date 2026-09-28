package com.example.datnhathub.controller;

import com.example.datnhathub.dto.*;
import com.example.datnhathub.entity.*;
import com.example.datnhathub.repository.ProductDetailRepository;
import com.example.datnhathub.repository.VoucherRepository;
import com.example.datnhathub.service.PosService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/*
 * LƯU Ý: bỏ @RequestMapping("/pos") ở mức class (so với bản gốc) vì controller này giờ
 * phục vụ CẢ 2 nhóm route:
 *   - /pos/**        → trang POS dành cho Employee (giao diện đơn giản, sidebar riêng)
 *   - /admin/pos/**   → trang POS dành cho Admin (giao diện quản trị, cùng sidebar với
 *                       các trang admin khác: products, orders, vouchers...)
 * Cả 2 trang dùng chung toàn bộ API bên dưới (/pos/checkout, /pos/customer,
 * /pos/api/customer, /pos/orders/{id}/shipping-status) — KHÔNG tách API riêng cho admin,
 * để tránh trùng lặp logic thanh toán/khách hàng.
 *
 * MỚI: /admin/returns/lookup-pos và /admin/returns/counter-pos — tra cứu + xử lý TRẢ HÀNG
 * cho đúng đơn BÁN TẠI QUẦY (PosOrder/PosOrderDetail), phục vụ nút "+ Thêm" ở trang
 * admin/returns.html khi khách mang hàng mua tại quầy đến trả trực tiếp.
 */
@Controller
public class PosController {

    @Autowired
    private PosService posService;

    @Autowired
    private com.example.datnhathub.repository.PosOrderRepository posOrderRepository;

    @Autowired
    private ProductDetailRepository productDetailRepository;

    @Autowired
    private VoucherRepository voucherRepository;

    // Chỉ ADMIN hoặc EMPLOYEE được vào bán hàng tại quầy (dùng cho trang /pos của Employee)
    private boolean isStaff(HttpSession session) {
        String roleName = (String) session.getAttribute("roleName");
        return "ADMIN".equals(roleName) || "EMPLOYEE".equals(roleName);
    }

    // Chỉ ADMIN (RoleID = 1) — dùng cho các trang /admin/pos/**, đồng bộ cách kiểm tra
    // quyền với các controller admin khác (AdminProductController, AdminController...)
    private boolean isAdmin(HttpSession session) {
        Users user = (Users) session.getAttribute("user");
        return user != null && user.getRole() != null && user.getRole().getRoleId().equals(1);
    }

    // ════════════════════════════════════════════════════════════
    // ── EMPLOYEE: /pos ──
    // ════════════════════════════════════════════════════════════

    // GET /pos — Trang bán hàng tại quầy (giao diện Employee)
    @GetMapping("/pos")
    public String posPage(HttpSession session, Model model) {
        Users user = (Users) session.getAttribute("user");
        if (user == null) return "redirect:/login";
        if (!isStaff(session)) return "access-denied";

        model.addAttribute("variants", posService.getAllVariantsForPos());
        return "employee/pos";
    }

    // GET /pos/history — Lịch sử đơn POS (giao diện Employee)
    @GetMapping("/pos/history")
    public String posHistory(HttpSession session, Model model) {
        Users user = (Users) session.getAttribute("user");
        if (user == null) return "redirect:/login";
        if (!isStaff(session)) return "access-denied";

        List<PosOrder> posOrders = posService.getAllPosOrders();
        model.addAttribute("posOrders", posOrders);
        model.addAttribute("totalRevenue", posService.getTotalRevenue(posOrders));
        return "employee/pos-history";
    }

    // ════════════════════════════════════════════════════════════
    // ── ADMIN: /admin/pos ──
    // ════════════════════════════════════════════════════════════

    // GET /admin/pos — Trang bán hàng tại quầy (giao diện Admin, cùng sidebar quản trị)
    @GetMapping("/admin/pos")
    public String adminPosPage(HttpSession session, Model model) {
        Users user = (Users) session.getAttribute("user");
        if (user == null) return "redirect:/login";
        if (!isAdmin(session)) return "access-denied";

        model.addAttribute("variants", posService.getAllVariantsForPos());
        return "admin/pos";
    }

    // GET /admin/pos/history — ĐÃ GỘP vào trang /admin/orders (tab "Đơn tại quầy (POS)").
    // Giữ route này chỉ để link/bookmark cũ vẫn hoạt động, tự chuyển hướng sang trang gộp.
    @GetMapping("/admin/pos/history")
    public String adminPosHistory() {
        return "redirect:/admin/orders?tab=offline";
    }

    // ════════════════════════════════════════════════════════════
    // ── TRẢ HÀNG TẠI QUẦY (MỚI) — dùng cho nút "+ Thêm" ở admin/returns.html ──
    // Khác với trả hàng đơn ONLINE (ReturnRequestController xử lý Orders/OrderDetail),
    // đây tra cứu và xử lý trả hàng cho đơn BÁN TẠI QUẦY (PosOrder/PosOrderDetail) —
    // đúng nghiệp vụ: khách mua trực tiếp tại quầy, sau đó quay lại trả hàng tại quầy.
    // Nhân viên tra cứu theo mã đơn POS (VD: POS20260819-15), chọn sản phẩm + số lượng
    // muốn trả, xử lý NGAY (hoàn kho + hoàn lượt voucher nếu có) — không qua bước chờ
    // duyệt vì nhân viên đã trực tiếp nhận lại hàng.
    // ════════════════════════════════════════════════════════════

    // GET /admin/returns/lookup-pos?orderCode=POS20260819-15 — Tra cứu đơn POS (AJAX)
    @GetMapping("/admin/returns/lookup-pos")
    @ResponseBody
    public Map<String, Object> lookupPosOrderForReturn(@RequestParam String orderCode, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        if (!isAdmin(session)) {
            result.put("success", false);
            result.put("message", "Bạn không có quyền thực hiện thao tác này.");
            return result;
        }

        Integer posOrderId = parsePosOrderIdFromCode(orderCode);
        PosOrder order = posOrderId != null ? posOrderRepository.findById(posOrderId).orElse(null) : null;

        if (order == null) {
            result.put("success", false);
            result.put("message", "Không tìm thấy đơn bán tại quầy với mã \"" + orderCode + "\"!");
            return result;
        }

        if (order.getReturnStatus() != null) {
            result.put("success", false);
            result.put("message", "Đơn này đã được xử lý trả hàng trước đó (trạng thái: " + order.getReturnStatus() + ").");
            return result;
        }

        List<Map<String, Object>> details = new ArrayList<>();
        if (order.getDetails() != null) {
            for (PosOrderDetail d : order.getDetails()) {
                Map<String, Object> item = new HashMap<>();
                item.put("posOrderDetailId", d.getPosOrderDetailId());
                item.put("productName", d.getProductDetail().getProduct().getProductName());
                item.put("size", d.getProductDetail().getSize());
                item.put("color", d.getProductDetail().getColor());
                item.put("quantity", d.getQuantity());
                details.add(item);
            }
        }

        result.put("success", true);
        result.put("posOrderId", order.getPosOrderId());
        result.put("orderCode", order.getOrderCode());
        result.put("customerName", order.getPosCustomer() != null ? order.getPosCustomer().getFullName() : "Khách lẻ");
        result.put("customerPhone", order.getPosCustomer() != null ? order.getPosCustomer().getPhone() : "");
        result.put("details", details);
        return result;
    }

    // POST /admin/returns/counter-pos — Xử lý trả hàng NGAY cho đơn bán tại quầy
    @Transactional
    @PostMapping("/admin/returns/counter-pos")
    public String submitPosCounterReturn(@RequestParam Integer posOrderId,
                                         @RequestParam String reason,
                                         @RequestParam List<Integer> posOrderDetailIds,
                                         @RequestParam List<Integer> quantities,
                                         HttpSession session,
                                         RedirectAttributes ra) {
        if (!isAdmin(session)) {
            return "access-denied";
        }

        PosOrder order = posOrderRepository.findById(posOrderId).orElse(null);
        if (order == null) {
            ra.addFlashAttribute("error", "Không tìm thấy đơn bán tại quầy!");
            return "redirect:/admin/returns";
        }

        if (order.getReturnStatus() != null) {
            ra.addFlashAttribute("error", "Đơn này đã được xử lý trả hàng trước đó!");
            return "redirect:/admin/returns";
        }

        if (posOrderDetailIds == null || quantities == null || posOrderDetailIds.size() != quantities.size()) {
            ra.addFlashAttribute("error", "Dữ liệu trả hàng không hợp lệ!");
            return "redirect:/admin/returns";
        }

        Map<Integer, PosOrderDetail> detailMap = new HashMap<>();
        if (order.getDetails() != null) {
            for (PosOrderDetail d : order.getDetails()) {
                detailMap.put(d.getPosOrderDetailId(), d);
            }
        }

        boolean anySelected = false;
        for (int i = 0; i < posOrderDetailIds.size(); i++) {
            Integer detailId = posOrderDetailIds.get(i);
            Integer qty = quantities.get(i);
            if (qty == null || qty <= 0) continue;

            PosOrderDetail detail = detailMap.get(detailId);
            if (detail == null) continue;

            if (qty > detail.getQuantity()) {
                ra.addFlashAttribute("error",
                        "Số lượng trả của \"" + detail.getProductDetail().getProduct().getProductName()
                                + "\" vượt quá số lượng đã mua!");
                return "redirect:/admin/returns";
            }

            detail.setReturnQuantity(qty);
            anySelected = true;
        }

        if (!anySelected) {
            ra.addFlashAttribute("error", "Vui lòng chọn ít nhất 1 sản phẩm và số lượng muốn trả!");
            return "redirect:/admin/returns";
        }

        // Hoàn kho — CHỈ cộng đúng số lượng khách trả ở từng dòng (hỗ trợ trả một phần)
        for (PosOrderDetail d : order.getDetails()) {
            if (d.getReturnQuantity() != null && d.getReturnQuantity() > 0) {
                ProductDetail pd = d.getProductDetail();
                pd.setStockQuantity(pd.getStockQuantity() + d.getReturnQuantity());
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
        order.setReturnReason(reason == null || reason.isBlank() ? "Trả hàng tại quầy" : reason);
        order.setReturnDate(java.time.LocalDateTime.now());
        posOrderRepository.save(order);

        ra.addFlashAttribute("success", "Đã ghi nhận trả hàng cho đơn bán tại quầy #" + order.getOrderCode() + "!");
        return "redirect:/admin/returns";
    }

    // Mã đơn POS có dạng "POS" + yyyyMMdd + "-" + posOrderId, VD: "POS20260819-15".
    // Có dấu "-" phân cách rõ ràng nên chỉ cần lấy phần sau dấu "-" cuối cùng là ID thật.
    // Cho phép nhập kèm "#", hoặc nhập thẳng posOrderId (không có tiền tố "POS").
    private Integer parsePosOrderIdFromCode(String codeOrId) {
        if (codeOrId == null) return null;
        String s = codeOrId.trim();
        if (s.startsWith("#")) s = s.substring(1);

        if (s.toUpperCase().startsWith("POS")) {
            int dashIdx = s.lastIndexOf('-');
            if (dashIdx == -1 || dashIdx == s.length() - 1) return null;
            String idPart = s.substring(dashIdx + 1);
            try {
                return Integer.parseInt(idPart);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ════════════════════════════════════════════════════════════
    // ── API DÙNG CHUNG cho cả /pos (Employee) và /admin/pos (Admin) ──
    // ════════════════════════════════════════════════════════════

    // POST /pos/orders/{id}/shipping-status — Cập nhật trạng thái giao hàng tận nơi
    @PostMapping("/pos/orders/{id}/shipping-status")
    public String updateShippingStatus(@PathVariable Integer id,
                                       @RequestParam String status,
                                       HttpSession session,
                                       RedirectAttributes ra,
                                       jakarta.servlet.http.HttpServletRequest request) {
        Users user = (Users) session.getAttribute("user");
        if (user == null) return "redirect:/login";
        if (!isStaff(session)) return "access-denied";

        try {
            posService.updateShippingStatus(id, status);
            ra.addFlashAttribute("success", "Đã cập nhật trạng thái giao hàng!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }

        String referer = request.getHeader("Referer");
        if (referer != null && referer.contains("/admin/orders")) {
            return "redirect:/admin/orders?tab=offline";
        }
        return "redirect:/pos/history";
    }

    // GET /pos/api/customer?phone=xxx — Tra cứu khách hàng theo SĐT (AJAX)
    @GetMapping("/pos/api/customer")
    @ResponseBody
    public ResponseEntity<?> lookupCustomer(@RequestParam String phone) {
        Optional<PosCustomer> customer = posService.findByPhone(phone);
        if (customer.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toDto(customer.get()));
    }

    // POST /pos/customer — Tạo khách hàng POS mới (AJAX)
    @PostMapping("/pos/customer")
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

    // POST /pos/checkout — Thanh toán đơn tại quầy (AJAX) — dùng chung cho cả 2 giao diện
    @PostMapping("/pos/checkout")
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