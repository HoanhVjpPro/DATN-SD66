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
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    // Cho phép chọn TỪNG sản phẩm (orderDetailIds) kèm số lượng muốn trả (quantities),
    // thay vì bắt buộc trả toàn bộ đơn hàng. Ví dụ: đặt 3 sản phẩm nhưng cửa hàng chỉ
    // giao sai 1 sản phẩm -> khách chỉ tick chọn đúng sản phẩm đó và nhập số lượng cần trả.
    @PostMapping("/orders/{id}/return")
    public String submitReturn(@PathVariable Integer id,
                               @RequestParam String reason,
                               @RequestParam List<Integer> orderDetailIds,
                               @RequestParam List<Integer> quantities,
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

        if (orderDetailIds == null || quantities == null || orderDetailIds.size() != quantities.size()) {
            ra.addFlashAttribute("error", "Dữ liệu trả hàng không hợp lệ!");
            return "redirect:/orders/" + id + "/return";
        }

        Map<Integer, OrderDetail> detailMap = order.getDetails().stream()
                .collect(Collectors.toMap(OrderDetail::getOrderDetailId, d -> d));

        boolean anySelected = false;
        for (int i = 0; i < orderDetailIds.size(); i++) {
            Integer detailId = orderDetailIds.get(i);
            Integer qty = quantities.get(i);
            if (qty == null || qty <= 0) continue;

            OrderDetail detail = detailMap.get(detailId);
            if (detail == null) continue;

            if (qty > detail.getQuantity()) {
                ra.addFlashAttribute("error",
                        "Số lượng trả của \"" + detail.getProductDetail().getProduct().getProductName()
                                + "\" vượt quá số lượng đã đặt!");
                return "redirect:/orders/" + id + "/return";
            }

            detail.setReturnQuantity(qty);
            anySelected = true;
        }

        if (!anySelected) {
            ra.addFlashAttribute("error", "Vui lòng chọn ít nhất 1 sản phẩm và số lượng muốn trả!");
            return "redirect:/orders/" + id + "/return";
        }

        order.setReturnStatus("Chờ xác nhận");
        order.setReturnReason(reason);
        order.setReturnDate(LocalDateTime.now());
        ordersRepository.save(order); // cascade ALL -> lưu luôn returnQuantity của các OrderDetail đã chỉnh

        ra.addFlashAttribute("success", "Đã gửi yêu cầu trả hàng!");
        return "redirect:/orders/" + id + "/return";
    }

    // ── Admin xem danh sách yêu cầu trả hàng ──
    @GetMapping("/admin/returns")
    public String adminReturnList(Model model) {
        // Lấy các đơn có ReturnStatus != null
        List<Orders> returns = ordersRepository.findByReturnStatusNotNullOrderByReturnDateDesc();
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

        restockAndRefundVoucher(order);

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

    // ════════════════════════════════════════════════════════════
    // ── TRẢ HÀNG TẠI QUẦY (MỚI) ──
    // Dành cho trường hợp khách mang hàng đến trực tiếp cửa hàng để trả sau khi đơn
    // online đã "Hoàn thành" — nhân viên/admin tra cứu theo mã đơn ngay tại quầy và xử
    // lý trả hàng luôn (không cần khách tự gửi yêu cầu qua website và chờ duyệt).
    // Do nhân viên đã trực tiếp nhận lại hàng nên yêu cầu được TẠO và CHẤP NHẬN ngay
    // trong cùng 1 thao tác — không cần bước "Chờ xác nhận" như luồng online.
    // ════════════════════════════════════════════════════════════

    // GET /admin/returns/lookup?orderCode=HH2026082312 — Tra cứu đơn hàng theo mã đơn (AJAX)
    @GetMapping("/admin/returns/lookup")
    @ResponseBody
    public Map<String, Object> lookupOrderForCounterReturn(@RequestParam String orderCode) {
        Map<String, Object> result = new HashMap<>();

        Integer orderId = parseOrderIdFromCode(orderCode);
        Orders order = orderId != null ? ordersRepository.findById(orderId).orElse(null) : null;

        if (order == null) {
            result.put("success", false);
            result.put("message", "Không tìm thấy đơn hàng với mã \"" + orderCode + "\"!");
            return result;
        }

        if (!"Hoàn thành".equals(order.getStatus())) {
            result.put("success", false);
            result.put("message", "Chỉ có thể trả hàng cho đơn đã ở trạng thái \"Hoàn thành\" (đơn này đang: "
                    + order.getStatus() + ").");
            return result;
        }

        if (order.getReturnStatus() != null) {
            result.put("success", false);
            result.put("message", "Đơn hàng này đã có yêu cầu trả hàng (trạng thái: " + order.getReturnStatus() + ").");
            return result;
        }

        List<Map<String, Object>> details = new ArrayList<>();
        if (order.getDetails() != null) {
            for (OrderDetail d : order.getDetails()) {
                Map<String, Object> item = new HashMap<>();
                item.put("orderDetailId", d.getOrderDetailId());
                item.put("productName", d.getProductDetail().getProduct().getProductName());
                item.put("size", d.getProductDetail().getSize());
                item.put("color", d.getProductDetail().getColor());
                item.put("quantity", d.getQuantity());
                details.add(item);
            }
        }

        result.put("success", true);
        result.put("orderId", order.getOrderId());
        result.put("orderCode", order.getOrderCode());
        result.put("customerName", order.getCustomer() != null && order.getCustomer().getUser() != null
                ? order.getCustomer().getUser().getUsername() : "—");
        result.put("customerPhone", order.getCustomer() != null && order.getCustomer().getUser() != null
                ? order.getCustomer().getUser().getPhone() : "");
        result.put("details", details);
        return result;
    }

    // POST /admin/returns/counter — Tạo VÀ chấp nhận yêu cầu trả hàng tại quầy trong 1 bước
    @Transactional
    @PostMapping("/admin/returns/counter")
    public String submitCounterReturn(@RequestParam Integer orderId,
                                      @RequestParam String reason,
                                      @RequestParam List<Integer> orderDetailIds,
                                      @RequestParam List<Integer> quantities,
                                      RedirectAttributes ra) {

        Orders order = ordersRepository.findById(orderId).orElse(null);
        if (order == null) {
            ra.addFlashAttribute("error", "Không tìm thấy đơn hàng!");
            return "redirect:/admin/returns";
        }

        if (!"Hoàn thành".equals(order.getStatus())) {
            ra.addFlashAttribute("error", "Chỉ trả hàng khi đơn đã hoàn thành!");
            return "redirect:/admin/returns";
        }

        if (order.getReturnStatus() != null) {
            ra.addFlashAttribute("error", "Đơn hàng này đã có yêu cầu trả hàng!");
            return "redirect:/admin/returns";
        }

        if (orderDetailIds == null || quantities == null || orderDetailIds.size() != quantities.size()) {
            ra.addFlashAttribute("error", "Dữ liệu trả hàng không hợp lệ!");
            return "redirect:/admin/returns";
        }

        Map<Integer, OrderDetail> detailMap = order.getDetails().stream()
                .collect(Collectors.toMap(OrderDetail::getOrderDetailId, d -> d));

        boolean anySelected = false;
        for (int i = 0; i < orderDetailIds.size(); i++) {
            Integer detailId = orderDetailIds.get(i);
            Integer qty = quantities.get(i);
            if (qty == null || qty <= 0) continue;

            OrderDetail detail = detailMap.get(detailId);
            if (detail == null) continue;

            if (qty > detail.getQuantity()) {
                ra.addFlashAttribute("error",
                        "Số lượng trả của \"" + detail.getProductDetail().getProduct().getProductName()
                                + "\" vượt quá số lượng đã đặt!");
                return "redirect:/admin/returns";
            }

            detail.setReturnQuantity(qty);
            anySelected = true;
        }

        if (!anySelected) {
            ra.addFlashAttribute("error", "Vui lòng chọn ít nhất 1 sản phẩm và số lượng muốn trả!");
            return "redirect:/admin/returns";
        }

        order.setReturnStatus("Chờ xác nhận");
        order.setReturnReason(reason == null || reason.isBlank() ? "Trả hàng tại quầy" : reason);
        order.setReturnDate(LocalDateTime.now());

        // Trả tại quầy -> nhân viên đã trực tiếp nhận lại hàng nên chấp nhận NGAY,
        // dùng chung logic hoàn kho/hoàn voucher với acceptReturn() phía admin online.
        restockAndRefundVoucher(order);
        order.setReturnStatus("Đã chấp nhận");

        ordersRepository.save(order);

        ra.addFlashAttribute("success", "Đã ghi nhận trả hàng tại quầy cho đơn #" + order.getOrderCode() + "!");
        return "redirect:/admin/returns";
    }

    // Hoàn lại tồn kho (đúng số lượng khách trả) + hoàn lượt sử dụng voucher nếu đơn có dùng.
    // Dùng chung cho cả acceptReturn() (luồng online) và submitCounterReturn() (luồng tại quầy).
    private void restockAndRefundVoucher(Orders order) {
        if (order.getDetails() != null) {
            for (OrderDetail od : order.getDetails()) {
                if (od.getReturnQuantity() != null && od.getReturnQuantity() > 0) {
                    ProductDetail pd = od.getProductDetail();
                    pd.setStockQuantity(pd.getStockQuantity() + od.getReturnQuantity());
                    productDetailRepository.save(pd);
                }
            }
        }

        if (order.getVoucher() != null) {
            Voucher v = order.getVoucher();
            v.setQuantity((v.getQuantity() == null ? 0 : v.getQuantity()) + 1);
            voucherRepository.save(v);
        }
    }

    // Mã đơn có dạng "HH" + yyyyMMdd (8 chữ số) + orderId (nối liền, không dấu phân cách),
    // ví dụ orderId=12, ngày 23/08/2026 -> "HH2026082312". Vì phần ngày luôn đúng 8 ký tự,
    // có thể tách chắc chắn: bỏ "HH", 8 ký tự đầu là ngày, phần còn lại là orderId thật.
    // Cho phép nhập kèm "#", hoặc nhập thẳng orderId (không có tiền tố "HH").
    private Integer parseOrderIdFromCode(String codeOrId) {
        if (codeOrId == null) return null;
        String s = codeOrId.trim();
        if (s.startsWith("#")) s = s.substring(1);

        if (s.toUpperCase().startsWith("HH")) {
            s = s.substring(2);
            if (s.length() <= 8) return null; // thiếu phần orderId sau ngày
            String idPart = s.substring(8);
            try {
                return Integer.parseInt(idPart);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        // Không có tiền tố "HH" -> coi như nhập thẳng OrderID
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}