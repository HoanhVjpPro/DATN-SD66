package com.example.datnhathub.service;

import com.example.datnhathub.entity.*;
import com.example.datnhathub.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {

    @Autowired private OrderRepository orderRepository;
    @Autowired private CartService cartService;
    @Autowired private CartDetailRepository cartDetailRepository;
    @Autowired private VoucherRepository voucherRepository;
    @Autowired private ProductDetailRepository productDetailRepository;
    @Autowired private EmployeeRepository employeeRepository;

    // ════════════════════════════════════════
    // UC20 — Đặt hàng từ giỏ hàng
    // FIX: validate tồn kho lại lần cuối + trừ kho sau khi đặt thành công
    // ════════════════════════════════════════
    @Transactional
    public Orders placeOrder(Integer userId, String shippingAddress, String paymentMethod, String voucherCode) {

        Customer customer = cartService.getCustomerByUserId(userId);
        Cart cart = cartService.getOrCreateCart(customer);

        if (cart.getDetails() == null || cart.getDetails().isEmpty()) {
            throw new RuntimeException("Giỏ hàng đang trống, không thể đặt hàng.");
        }

        // FIX: validate lại tồn kho lần cuối trước khi tạo đơn
        // (đề phòng trường hợp giữa lúc thêm vào giỏ và lúc đặt hàng, người khác đã mua hết)
        for (CartDetail cd : cart.getDetails()) {
            ProductDetail pd = cd.getProductDetail();
            if (pd.getStockQuantity() == null || cd.getQuantity() > pd.getStockQuantity()) {
                throw new RuntimeException(
                        "Sản phẩm \"" + pd.getProduct().getProductName() + "\" (" + pd.getSize() + "/" + pd.getColor()
                                + ") chỉ còn " + (pd.getStockQuantity() == null ? 0 : pd.getStockQuantity())
                                + " trong kho. Vui lòng cập nhật lại giỏ hàng."
                );
            }
        }

        BigDecimal total = cartService.calculateTotal(cart);

        // Áp dụng voucher nếu có
        Voucher voucher = null;
        if (voucherCode != null && !voucherCode.isBlank()) {
            voucher = voucherRepository.findByCode(voucherCode.trim())
                    .orElseThrow(() -> new RuntimeException("Mã voucher không hợp lệ."));

            if (voucher.getQuantity() == null || voucher.getQuantity() <= 0) {
                throw new RuntimeException("Voucher đã hết số lượng sử dụng.");
            }

            total = total.subtract(voucher.getDiscountAmount());
            if (total.compareTo(BigDecimal.ZERO) < 0) {
                total = BigDecimal.ZERO;
            }
        }

        // Tạo Order
        Orders order = new Orders();
        order.setCustomer(customer);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus("Chờ xác nhận");
        order.setTotalAmount(total);

        // Tạo Order_Detail từ Cart_Detail + trừ kho ngay tại đây
        List<OrderDetail> orderDetails = new ArrayList<>();
        for (CartDetail cd : cart.getDetails()) {
            ProductDetail pd = cd.getProductDetail();

            OrderDetail od = new OrderDetail();
            od.setOrder(order);
            od.setProductDetail(pd);
            od.setQuantity(cd.getQuantity());
            od.setUnitPrice(pd.getPrice());
            orderDetails.add(od);

            // FIX: trừ kho ngay sau khi xác nhận đặt hàng thành công
            pd.setStockQuantity(pd.getStockQuantity() - cd.getQuantity());
            productDetailRepository.save(pd);
        }
        order.setDetails(orderDetails);

        // Shipping
        Shipping shipping = new Shipping();
        shipping.setOrder(order);
        shipping.setShippingAddress(shippingAddress);
        shipping.setShippingStatus("Chưa giao");
        order.setShipping(shipping);

        // Payment
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentMethod(paymentMethod);
        payment.setPaymentStatus("COD".equals(paymentMethod) ? "Chưa thanh toán" : "Chờ xác nhận");
        order.setPayment(payment);

        Orders savedOrder = orderRepository.save(order); // cascade lưu cả OrderDetail, Shipping, Payment

        // Trừ số lượng voucher nếu dùng
        if (voucher != null) {
            voucher.setQuantity(voucher.getQuantity() - 1);
            voucherRepository.save(voucher);
        }

        // Xóa hết Cart_Detail sau khi đặt hàng thành công (giỏ hàng trống lại)
        cartDetailRepository.deleteByCartCartId(cart.getCartId());
        return savedOrder;
    }

    public List<Orders> getOrdersByUserId(Integer userId) {
        Customer customer = cartService.getCustomerByUserId(userId);
        return orderRepository.findByCustomerCustomerIdOrderByOrderDateDesc(customer.getCustomerId());
    }
//VND
    public long countByStatus(String status) {
        return orderRepository.countByStatus(status);
    }

    public long countAll() {
        return orderRepository.count();
    }

    @Transactional(readOnly = true)
    public List<Orders> getAllOrders() {
        return orderRepository.findAllByOrderByOrderDateDesc();
    }

    @Transactional
    public void updateOrderStatus(Integer orderId, String status, Integer employeeUserId) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại"));
        order.setStatus(status);

        if (employeeUserId != null) {
            employeeRepository.findByUserUserID(employeeUserId).ifPresent(order::setEmployee);
        }

        if ("Đang giao".equals(status) && order.getShipping() != null) {
            order.getShipping().setShippingStatus("Đang giao");
        }
        if ("Hoàn thành".equals(status)) {
            if (order.getShipping() != null) order.getShipping().setShippingStatus("Đã giao");
            if (order.getPayment() != null) order.getPayment().setPaymentStatus("Đã thanh toán");
        }
        if ("Đã hủy".equals(status)) {
            if (order.getShipping() != null) order.getShipping().setShippingStatus("Đã hủy");
        }

        orderRepository.save(order);
    }
}