package com.example.datnhathub.controller;

import com.example.datnhathub.entity.Customer;
import com.example.datnhathub.entity.OrderDetail;
import com.example.datnhathub.entity.Orders;
import com.example.datnhathub.entity.ProductDetail;
import com.example.datnhathub.entity.Reviews;
import com.example.datnhathub.repository.OrderRepository;
import com.example.datnhathub.repository.ProductDetailRepository;
import com.example.datnhathub.repository.ReviewRepository;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Controller
public class OrderController {
    @Autowired
    private OrderRepository ordersRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private ReviewRepository reviewRepository;

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

    // ── Customer đánh giá sản phẩm ngay từ trang chi tiết đơn hàng (kèm ảnh tùy chọn) ──
    // Chỉ cho đánh giá khi đơn thuộc customer đang đăng nhập, đã "Hoàn thành",
    // và biến thể được đánh giá thực sự nằm trong đơn đó. Mỗi (biến thể, khách) chỉ 1 review:
    // nếu đã có thì cập nhật lại (khớp UNIQUE constraint UQ_Review_ProductDetail_Customer).
    @PostMapping("/ordercomment")
    public String orderComment(@RequestParam("orderid") Integer orderId,
                               @RequestParam("ProductDetail") Integer productDetailId,
                               @RequestParam("Rating") Double rating,
                               @RequestParam(value = "Comment", required = false, defaultValue = "") String comment,
                               @RequestParam(value = "imgvl", required = false) MultipartFile file,
                               HttpSession session,
                               RedirectAttributes ra) {

        Integer customerId = (Integer) session.getAttribute("customerId");
        if (customerId == null) return "redirect:/login";

        Orders order = ordersRepository.findById(orderId).orElse(null);
        if (order == null || !order.getCustomer().getCustomerId().equals(customerId)) {
            ra.addFlashAttribute("error", "Không tìm thấy đơn hàng!");
            return "redirect:/orders";
        }

        if (!"Hoàn thành".equals(order.getStatus())) {
            ra.addFlashAttribute("error", "Chỉ đánh giá được khi đơn hàng đã hoàn thành!");
            return "redirect:/orders/" + orderId;
        }

        if (rating == null || rating < 1 || rating > 5) {
            ra.addFlashAttribute("error", "Số sao đánh giá không hợp lệ!");
            return "redirect:/orders/" + orderId;
        }

        OrderDetail matched = order.getDetails() == null ? null : order.getDetails().stream()
                .filter(d -> d.getProductDetail().getProductDetailId().equals(productDetailId))
                .findFirst().orElse(null);
        if (matched == null) {
            ra.addFlashAttribute("error", "Sản phẩm này không thuộc đơn hàng!");
            return "redirect:/orders/" + orderId;
        }

        Customer customer = order.getCustomer();
        ProductDetail pd = matched.getProductDetail();

        // Lưu ảnh (nếu có) vào uploads/feedbackIMG — Webconfig đã map /uploads/** ra thư mục này
        String imgValue = null;
        if (file != null && !file.isEmpty()) {
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                ra.addFlashAttribute("error", "File tải lên phải là hình ảnh!");
                return "redirect:/orders/" + orderId;
            }
            try {
                String original = file.getOriginalFilename();
                String ext = ".jpg";
                if (original != null && original.contains(".")) {
                    String e = original.substring(original.lastIndexOf('.')).toLowerCase();
                    if (e.matches("\\.(jpg|jpeg|png|gif|webp)")) ext = e;
                }
                Path dir = Paths.get(System.getProperty("user.dir"), "uploads", "feedbackIMG");
                Files.createDirectories(dir);
                String filename = UUID.randomUUID() + ext;
                Files.copy(file.getInputStream(), dir.resolve(filename));
                imgValue = "/uploads/feedbackIMG/" + filename;
            } catch (IOException e) {
                ra.addFlashAttribute("error", "Không lưu được ảnh: " + e.getMessage());
                return "redirect:/orders/" + orderId;
            }
        }

        Reviews review = reviewRepository.findByCustomerIDAndProductDetailID(
                customer.getCustomerId(), productDetailId);
        if (review == null) {
            review = new Reviews();
            review.setCustomerID(customer);
            review.setProductDetailID(pd);
        }
        review.setRating(rating);
        review.setComment(comment);
        if (imgValue != null) {
            review.setReviewImage(imgValue); // không gửi ảnh mới -> giữ ảnh cũ
        }
        reviewRepository.save(review);

        ra.addFlashAttribute("success", "Cảm ơn bạn đã đánh giá!");
        return "redirect:/orders/" + orderId;
    }
}