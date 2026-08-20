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
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private ProductDetailRepository productDetailRepository;
    @Autowired
    private ShipperRepository shipperRepository;

    // Danh sách đơn "Đang giao" nhưng chưa ai nhận (pool cho shipper)
    public List<Orders> getUnclaimedOrders() {
        return orderRepository.findByStatusAndShippingShipperIsNull("Đang giao");
    }

    // Danh sách đơn shipper đang phụ trách (đã nhận, chưa xong)
    // Loại bỏ đơn "Đã hủy" (vd: CSKH đã xử lý hoàn tiền cho đơn gặp sự cố) khỏi danh sách của shipper
    public List<Orders> getOrdersForShipper(Integer shipperUserId) {
        Shipper shipper = shipperRepository.findByUserUserID(shipperUserId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy shipper"));
        return orderRepository.findByShippingShipperShipperIdOrderByOrderDateDesc(shipper.getShipperId())
                .stream()
                .filter(o -> !"Đã hủy".equals(o.getStatus()))
                .toList();
    }

    // Shipper nhận đơn (claim)
    @Transactional
    public void claimOrder(Integer orderId, Integer shipperUserId) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại"));

        if (!"Đang giao".equals(order.getStatus()) || order.getShipping() == null) {
            throw new IllegalStateException("Đơn hàng không ở trạng thái chờ giao");
        }
        if (order.getShipping().getShipper() != null) {
            throw new IllegalStateException("Đơn hàng đã có shipper khác nhận");
        }

        Shipper shipper = shipperRepository.findByUserUserID(shipperUserId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy shipper"));

        order.getShipping().setShipper(shipper);
        orderRepository.save(order);
    }

    // Shipper xác nhận đã giao thành công (khách chưa xác nhận thì chưa Hoàn thành)
    @Transactional
    public void confirmDeliveredByShipper(Integer orderId, Integer shipperUserId) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại"));

        validateShipperOwnsOrder(order, shipperUserId);

        order.getShipping().setConfirmedByShipper(true);
        orderRepository.save(order);
    }

    // Shipper báo sự cố (mất hàng / giao thất bại)
    @Transactional
    public void reportIncident(Integer orderId, Integer shipperUserId, String reason) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại"));

        validateShipperOwnsOrder(order, shipperUserId);

        order.setStatus("Xử lý sự cố giao hàng");
        order.getShipping().setIncidentReason(reason);
        order.getShipping().setShippingStatus("Sự cố");
        orderRepository.save(order);
    }

    private void validateShipperOwnsOrder(Orders order, Integer shipperUserId) {
        if (order.getShipping() == null || order.getShipping().getShipper() == null) {
            throw new IllegalStateException("Đơn hàng chưa được nhận bởi shipper nào");
        }
        Integer ownerUserId = order.getShipping().getShipper().getUser().getUserID();
        if (!ownerUserId.equals(shipperUserId)) {
            throw new IllegalStateException("Bạn không phải shipper phụ trách đơn này");
        }
    }

// ── Admin xử lý sự cố ──

    // Chọn "Liên hệ CSKH hoàn tiền": hủy đơn, hoàn kho/voucher qua updateOrderStatus, đánh dấu Payment cần CSKH xử lý
    @Transactional
    public void resolveIncidentByRefund(Integer orderId) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại"));

        if (!"Xử lý sự cố giao hàng".equals(order.getStatus())) {
            throw new IllegalStateException("Đơn hàng không ở trạng thái sự cố");
        }

        updateOrderStatus(orderId, "Đã hủy", null); // tái sử dụng logic hoàn kho + hoàn voucher

        if (order.getPayment() != null) {
            order.getPayment().setPaymentStatus("Cần hoàn tiền - Liên hệ CSKH");
            orderRepository.save(order);
        }
    }

    // Chọn "Giao lại": quay về pool, ai nhận cũng được, không trừ kho lần nữa (stockDeducted vẫn true)
    @Transactional
    public void resolveIncidentByReship(Integer orderId) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại"));

        if (!"Xử lý sự cố giao hàng".equals(order.getStatus())) {
            throw new IllegalStateException("Đơn hàng không ở trạng thái sự cố");
        }

        order.setStatus("Đang giao");
        if (order.getShipping() != null) {
            order.getShipping().setShipper(null); // quay lại pool
            order.getShipping().setConfirmedByShipper(false);
            order.getShipping().setIncidentReason(null);
            order.getShipping().setShippingStatus("Đang giao");
        }
        orderRepository.save(order);
    }

    // Đặt hàng từ giỏ hàng
    @Transactional
    public Orders placeOrder(Integer userId, String shippingAddress, String city,
                             String paymentMethod, String voucherCode,
                             List<Integer> selectedCartDetailIds) {

        Customer customer = cartService.getCustomerByUserId(userId);
        Cart cart = cartService.getOrCreateCart(customer);

        if (cart.getDetails() == null || cart.getDetails().isEmpty()) {
            throw new RuntimeException("Giỏ hàng đang trống, không thể đặt hàng.");
        }

        List<CartDetail> itemsToOrder = (selectedCartDetailIds == null || selectedCartDetailIds.isEmpty())
                ? cart.getDetails()
                : cart.getDetails().stream()
                .filter(cd -> selectedCartDetailIds.contains(cd.getCartDetailId()))
                .toList();

        if (itemsToOrder.isEmpty()) {
            throw new RuntimeException("Không có sản phẩm nào được chọn để đặt hàng.");
        }

        for (CartDetail cd : itemsToOrder) {
            ProductDetail pd = cd.getProductDetail();
            int stock = pd.getStockQuantity() == null ? 0 : pd.getStockQuantity();
            if (cd.getQuantity() > stock) {
                throw new RuntimeException("Sản phẩm \"" + pd.getProduct().getProductName() + "\" ("
                        + pd.getSize() + "/" + pd.getColor() + ") không đủ số lượng trong kho.");
            }
        }

        BigDecimal subtotal = itemsToOrder.stream()
                .map(d -> d.getProductDetail().getPrice().multiply(BigDecimal.valueOf(d.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Phí ship tính trên subtotal (trước khi trừ voucher)
        BigDecimal shippingFee = calculateShippingFee(city, subtotal);

        BigDecimal total = subtotal;

        Voucher voucher = null;
        if (voucherCode != null && !voucherCode.isBlank()) {
            voucher = voucherRepository.findByCode(voucherCode.trim())
                    .orElseThrow(() -> new RuntimeException("Mã voucher không hợp lệ."));

            if (voucher.getQuantity() == null || voucher.getQuantity() <= 0) {
                throw new RuntimeException("Voucher đã hết số lượng sử dụng.");
            }
            if (voucher.isExpired()) {
                throw new RuntimeException("Voucher đã hết hạn sử dụng.");
            }

            total = total.subtract(voucher.calculateDiscount(subtotal));
            if (total.compareTo(BigDecimal.ZERO) < 0) {
                total = BigDecimal.ZERO;
            }

            if (voucher.isFreeShip()) {
                shippingFee = BigDecimal.ZERO; // ghi đè phí ship đã tính theo thành phố
            }
        }

        total = total.add(shippingFee);

        Orders order = new Orders();
        order.setTotalAmount(total);
        order.setVoucher(voucher); // lưu lại voucher đã áp dụng (null nếu không dùng)
        order.setCustomer(customer);
        order.setOrderDate(LocalDateTime.now());
        boolean needPayment = "Chuyển khoản".equals(paymentMethod)
                && total.compareTo(BigDecimal.ZERO) > 0;
        order.setStatus(needPayment ? "Chờ thanh toán" : "Chờ xác nhận");
        order.setTotalAmount(total);
        order.setStockDeducted(false);

        List<OrderDetail> orderDetails = new ArrayList<>();
        for (CartDetail cd : itemsToOrder) {
            ProductDetail pd = cd.getProductDetail();
            OrderDetail od = new OrderDetail();
            od.setOrder(order);
            od.setProductDetail(pd);
            od.setQuantity(cd.getQuantity());
            od.setUnitPrice(pd.getPrice());
            orderDetails.add(od);
        }
        order.setDetails(orderDetails);

        Shipping shipping = new Shipping();
        shipping.setOrder(order);
        shipping.setShippingAddress(shippingAddress);
        shipping.setShippingFee(shippingFee);
        shipping.setShippingStatus("Chưa giao");
        order.setShipping(shipping);

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentMethod(paymentMethod);
        payment.setPaymentStatus("COD".equals(paymentMethod) ? "Chưa thanh toán" : "Chờ xác nhận");
        order.setPayment(payment);

        Orders savedOrder = orderRepository.save(order);

        if (voucher != null) {
            voucher.setQuantity(voucher.getQuantity() - 1);
            voucherRepository.save(voucher);
        }

        List<Integer> orderedDetailIds = itemsToOrder.stream().map(CartDetail::getCartDetailId).toList();
        cartDetailRepository.deleteAllById(orderedDetailIds);
        return savedOrder;
    }

    public List<Orders> getOrdersByUserId(Integer userId) {
        Customer customer = cartService.getCustomerByUserId(userId);
        return orderRepository.findByCustomerCustomerIdOrderByOrderDateDesc(customer.getCustomerId());
    }

    // tính phí ship
    private static final BigDecimal FREESHIP_THRESHOLD = new BigDecimal("500000");
    private static final BigDecimal FEE_HANOI = new BigDecimal("20000");
    private static final BigDecimal FEE_HCM = new BigDecimal("30000");
    private static final BigDecimal FEE_DANANG = new BigDecimal("25000");
    private static final BigDecimal FEE_OTHER = new BigDecimal("35000");

    private BigDecimal calculateShippingFee(String city, BigDecimal subtotal) {
        if (subtotal.compareTo(FREESHIP_THRESHOLD) >= 0) {
            return BigDecimal.ZERO;
        }
        if (city == null) return FEE_OTHER;
        String c = city.trim();
        if (c.equalsIgnoreCase("Hà Nội")) return FEE_HANOI;
        if (c.equalsIgnoreCase("TP. Hồ Chí Minh") || c.equalsIgnoreCase("Hồ Chí Minh")) return FEE_HCM;
        if (c.equalsIgnoreCase("Đà Nẵng")) return FEE_DANANG;
        return FEE_OTHER;
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

        String previousStatus = order.getStatus();

        if (employeeUserId != null) {
            employeeRepository.findByUserUserID(employeeUserId).ifPresent(order::setEmployee);
        }

        // Admin/nhân viên xác nhận đơn -> TRỪ KHO tại đây (lần đầu tiên đơn vào "Đang xử lý")
        if ("Đang xử lý".equals(status) && !Boolean.TRUE.equals(order.getStockDeducted())) {
            deductStockForOrder(order); // ném lỗi nếu không đủ hàng -> rollback toàn bộ transaction
            order.setStockDeducted(true);
        }

        // Nếu admin bấm "Quay lại" từ "Đang xử lý" về "Chờ xác nhận" -> hoàn lại kho đã trừ
        if ("Chờ xác nhận".equals(status) && "Đang xử lý".equals(previousStatus)
                && Boolean.TRUE.equals(order.getStockDeducted())) {
            restoreStockForOrder(order);
            order.setStockDeducted(false);
        }

        order.setStatus(status);

        if ("Đang giao".equals(status) && order.getShipping() != null) {
            order.getShipping().setShippingStatus("Đang giao");
        }

        if ("Hoàn thành".equals(status)) {
            if (order.getShipping() != null) order.getShipping().setShippingStatus("Đã giao");
            if (order.getPayment() != null) order.getPayment().setPaymentStatus("Đã thanh toán");
        }

        if ("Đã hủy".equals(status)) {
            if (order.getShipping() != null) order.getShipping().setShippingStatus("Đã hủy");

            // Hoàn kho nếu trước đó ĐÃ từng trừ (dùng cờ, không phụ thuộc chuỗi trạng thái)
            if (Boolean.TRUE.equals(order.getStockDeducted())) {
                restoreStockForOrder(order);
                order.setStockDeducted(false);
            }

            if (order.getVoucher() != null) {
                Voucher v = order.getVoucher();
                v.setQuantity((v.getQuantity() == null ? 0 : v.getQuantity()) + 1);
                voucherRepository.save(v);
            }
        }

        orderRepository.save(order);
    }

    // Trừ kho atomic cho từng dòng đơn hàng; ném lỗi (và rollback transaction) nếu bất kỳ sản phẩm nào không đủ hàng
    private void deductStockForOrder(Orders order) {
        if (order.getDetails() == null) return;
        for (OrderDetail od : order.getDetails()) {
            ProductDetail pd = od.getProductDetail();
            int updatedRows = productDetailRepository.decreaseStock(pd.getProductDetailId(), od.getQuantity());
            if (updatedRows == 0) {
                throw new IllegalStateException(
                        "Không thể xác nhận đơn: sản phẩm \"" + pd.getProduct().getProductName()
                                + "\" (" + pd.getSize() + "/" + pd.getColor() + ") không đủ tồn kho."
                );
            }
        }
    }

    // Hoàn lại kho cho toàn bộ đơn hàng (dùng khi hủy đơn hoặc revert về "Chờ xác nhận")
    private void restoreStockForOrder(Orders order) {
        if (order.getDetails() == null) return;
        for (OrderDetail od : order.getDetails()) {
            ProductDetail pd = od.getProductDetail();
            pd.setStockQuantity(pd.getStockQuantity() + od.getQuantity());
            productDetailRepository.save(pd);
        }
    }

    // Tự động hủy đơn "Chờ thanh toán" quá 15 phút chưa thanh toán
    private static final long PAYMENT_TIMEOUT_MINUTES = 15;

    @Transactional
    public int cancelExpiredUnpaidOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(PAYMENT_TIMEOUT_MINUTES);
        List<Orders> expired = orderRepository.findByStatusAndOrderDateBefore("Chờ thanh toán", cutoff);

        for (Orders order : expired) {
            updateOrderStatus(order.getOrderId(), "Đã hủy", null);
            if (order.getPayment() != null) {
                order.getPayment().setPaymentStatus("Đã hủy - Quá hạn thanh toán");
                orderRepository.save(order);
            }
        }
        return expired.size();
    }

    public List<OrderRepository.MonthlyRevenueProjection> getMonthlyRevenue() {
        return orderRepository.getMonthlyRevenue();
    }

    public List<OrderRepository.TopProductProjection> getTopSellingProducts() {
        return orderRepository.getTopSellingProducts();
    }

    public List<OrderRepository.TopProductProjection> getAllProductSales() {
        return orderRepository.getAllProductSales();
    }

    public List<OrderRepository.DailyRevenueProjection> getDailyRevenue() {
        return orderRepository.getDailyRevenue();
    }

    public List<OrderRepository.WeeklyRevenueProjection> getWeeklyRevenue() {
        return orderRepository.getWeeklyRevenue();
    }
}