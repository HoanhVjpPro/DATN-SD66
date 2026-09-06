package com.example.datnhathub.service;

import com.example.datnhathub.dto.PosCheckoutRequest;
import com.example.datnhathub.dto.PosVariantDto;
import com.example.datnhathub.entity.*;
import com.example.datnhathub.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PosService {

    @Autowired private PosCustomerRepository posCustomerRepository;
    @Autowired private PosOrderRepository posOrderRepository;
    @Autowired private ProductDetailRepository productDetailRepository;
    @Autowired private ProductImageRepository productImageRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private VoucherRepository voucherRepository;
    @Autowired private ProductService productService;

    // Bảng phí ship GỐC theo đơn vị vận chuyển (chưa áp freeship)
    private static final Map<String, BigDecimal> SHIPPING_FEES = Map.of(
            "GHTK", new BigDecimal("15000"),
            "GHN", new BigDecimal("30000")
    );

    // Đơn từ 500.000đ trở lên: GHTK miễn phí hoàn toàn, GHN giảm 15.000đ (giống ngưỡng freeship của OrderService online)
    private static final BigDecimal FREESHIP_THRESHOLD = new BigDecimal("500000");
    private static final BigDecimal GHN_FREESHIP_DISCOUNT = new BigDecimal("15000");

    // Tính phí ship thực tế theo đơn vị vận chuyển + tạm tính đơn hàng (áp dụng ưu đãi freeship nếu đủ điều kiện)
    private BigDecimal resolveShippingFee(String provider, BigDecimal subtotal) {
        BigDecimal baseFee = SHIPPING_FEES.get(provider);
        if (baseFee == null) {
            throw new RuntimeException("Đơn vị vận chuyển không hợp lệ.");
        }

        boolean eligibleForFreeship = subtotal.compareTo(FREESHIP_THRESHOLD) >= 0;
        if (!eligibleForFreeship) {
            return baseFee;
        }

        if ("GHTK".equals(provider)) {
            return BigDecimal.ZERO;
        }
        if ("GHN".equals(provider)) {
            BigDecimal discounted = baseFee.subtract(GHN_FREESHIP_DISCOUNT);
            return discounted.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : discounted;
        }
        return baseFee;
    }

    // Danh sách toàn bộ biến thể sản phẩm còn đang bán, đổ vào lưới sản phẩm trang POS
    public List<PosVariantDto> getAllVariantsForPos() {
        List<ProductDetail> details = productDetailRepository.findAll();
        List<PosVariantDto> result = new ArrayList<>();

        for (ProductDetail pd : details) {
            if (pd.getProduct() == null || !Boolean.TRUE.equals(pd.getProduct().getStatus())) continue;

            PosVariantDto dto = new PosVariantDto();
            dto.setProductDetailId(pd.getProductDetailId());
            dto.setProduct(pd.getProduct());
            dto.setSku(pd.getSku());
            dto.setSize(pd.getSize());
            dto.setColor(pd.getColor());
            dto.setPrice(pd.getPrice());
            dto.setStockQuantity(pd.getStockQuantity());
            dto.setDefaultImageUrl(resolveImageUrl(pd));
            result.add(dto);
        }
        return result;
    }

    // Ưu tiên ảnh gắn riêng cho biến thể, nếu không có thì lấy ảnh mặc định của sản phẩm
    private String resolveImageUrl(ProductDetail pd) {
        List<ProductImage> variantImages = productImageRepository.findByProductDetailProductDetailId(pd.getProductDetailId());
        if (variantImages != null && !variantImages.isEmpty()) {
            return variantImages.get(0).getImageURL();
        }
        return productService.getDefaultImageUrl(pd.getProduct());
    }

    // Tra cứu khách hàng POS theo SĐT
    public Optional<PosCustomer> findByPhone(String phone) {
        if (phone == null || phone.isBlank()) return Optional.empty();
        return posCustomerRepository.findByPhone(phone.trim());
    }

    // Tạo khách hàng POS mới (nếu SĐT đã tồn tại thì trả về bản ghi cũ, tránh tạo trùng)
    @Transactional
    public PosCustomer createCustomer(String phone, String fullName, String address, String email) {
        if (phone == null || phone.isBlank()) {
            throw new RuntimeException("Số điện thoại không được để trống.");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new RuntimeException("Họ tên khách hàng không được để trống.");
        }

        String trimmedPhone = phone.trim();
        Optional<PosCustomer> existing = posCustomerRepository.findByPhone(trimmedPhone);
        if (existing.isPresent()) {
            return existing.get();
        }

        PosCustomer customer = new PosCustomer();
        customer.setPhone(trimmedPhone);
        customer.setFullName(fullName.trim());
        customer.setAddress((address == null || address.isBlank()) ? null : address.trim());
        customer.setEmail((email == null || email.isBlank()) ? null : email.trim());
        customer.setCreatedDate(LocalDateTime.now());

        return posCustomerRepository.save(customer);
    }

    // Thanh toán đơn tại quầy: trừ kho atomic + áp dụng voucher (nếu có)
    // + ghi nhận giao hàng tận nơi (nếu có, áp dụng freeship theo ngưỡng 500k) + tạo POS_Order + POS_OrderDetail
    @Transactional
    public PosOrder checkout(PosCheckoutRequest req, Integer employeeUserId) {
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new RuntimeException("Đơn hàng chưa có sản phẩm nào.");
        }
        if (req.getPosCustomerId() == null) {
            throw new RuntimeException("Vui lòng chọn khách hàng trước khi thanh toán.");
        }

        PosCustomer customer = posCustomerRepository.findById(req.getPosCustomerId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng."));

        Employee employee = employeeRepository.findByUserUserID(employeeUserId)
                .orElseThrow(() -> new RuntimeException("Tài khoản hiện tại không có quyền bán hàng tại quầy."));

        PosOrder order = new PosOrder();
        order.setPosCustomer(customer);
        order.setEmployee(employee);
        order.setOrderDate(LocalDateTime.now());
        order.setPaymentMethod((req.getPaymentMethod() == null || req.getPaymentMethod().isBlank())
                ? "CASH" : req.getPaymentMethod());
        order.setStatus("Hoàn thành");

        List<PosOrderDetail> details = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (PosCheckoutRequest.PosCheckoutItem item : req.getItems()) {
            if (item.getProductDetailId() == null) continue;

            ProductDetail pd = productDetailRepository.findById(item.getProductDetailId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy biến thể sản phẩm ID: " + item.getProductDetailId()));

            int qty = item.getQuantity() == null ? 0 : item.getQuantity();
            if (qty < 1) {
                throw new RuntimeException("Số lượng không hợp lệ cho sản phẩm \"" + pd.getProduct().getProductName() + "\".");
            }

            int updatedRows = productDetailRepository.decreaseStock(pd.getProductDetailId(), qty);
            if (updatedRows == 0) {
                throw new RuntimeException("Sản phẩm \"" + pd.getProduct().getProductName() + "\" ("
                        + pd.getSize() + "/" + pd.getColor() + ") không đủ tồn kho.");
            }

            PosOrderDetail detail = new PosOrderDetail();
            detail.setPosOrder(order);
            detail.setProductDetail(pd);
            detail.setQuantity(qty);
            detail.setUnitPrice(pd.getPrice());
            details.add(detail);

            subtotal = subtotal.add(pd.getPrice().multiply(BigDecimal.valueOf(qty)));
        }

        // ── Áp dụng voucher (nếu có) — validate lại từ đầu, KHÔNG tin số liệu client gửi lên ──
        Voucher voucher = null;
        BigDecimal discount = BigDecimal.ZERO;

        if (req.getVoucherCode() != null && !req.getVoucherCode().isBlank()) {
            voucher = voucherRepository.findByCode(req.getVoucherCode().trim())
                    .orElseThrow(() -> new RuntimeException("Mã voucher không hợp lệ."));

            if (voucher.getQuantity() == null || voucher.getQuantity() <= 0) {
                throw new RuntimeException("Voucher đã hết số lượng sử dụng.");
            }
            if (voucher.isExpired()) {
                throw new RuntimeException("Voucher đã hết hạn sử dụng.");
            }

            discount = voucher.calculateDiscount(subtotal);
            if (discount.compareTo(subtotal) > 0) {
                discount = subtotal; // không để giảm giá vượt quá tổng tiền
            }
        }

        BigDecimal total = subtotal.subtract(discount);
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            total = BigDecimal.ZERO;
        }

        // ── Giao hàng tận nơi (nếu có) — chọn GHTK/GHN, phí tự tính theo tạm tính (áp dụng freeship nếu đủ 500k) ──
        boolean isShipping = Boolean.TRUE.equals(req.getIsShipping());
        String shippingAddress = null;
        String shippingProvider = null;
        BigDecimal shippingFee = null;
        String shippingStatus = null;

        if (isShipping) {
            if (req.getShippingAddress() == null || req.getShippingAddress().isBlank()) {
                throw new RuntimeException("Vui lòng nhập địa chỉ giao hàng.");
            }
            shippingAddress = req.getShippingAddress().trim();

            String providerRaw = req.getShippingProvider() == null ? "" : req.getShippingProvider().trim().toUpperCase();
            if (!SHIPPING_FEES.containsKey(providerRaw)) {
                throw new RuntimeException("Vui lòng chọn đơn vị vận chuyển (GHTK hoặc GHN).");
            }
            shippingProvider = providerRaw;

            // Phí ship tính trên TẠM TÍNH (subtotal, trước khi trừ voucher) — giống cách OrderService tính cho đơn online
            shippingFee = resolveShippingFee(providerRaw, subtotal);

            shippingStatus = "Chưa giao";
            total = total.add(shippingFee);
        }

        order.setDetails(details);
        order.setVoucher(voucher);
        order.setDiscountAmount(discount);
        order.setIsShipping(isShipping);
        order.setShippingAddress(shippingAddress);
        order.setShippingProvider(shippingProvider);
        order.setShippingFee(shippingFee);
        order.setShippingStatus(shippingStatus);
        order.setTotalAmount(total);

        PosOrder savedOrder = posOrderRepository.save(order);

        // Trừ lượt sử dụng voucher SAU khi lưu đơn thành công (cùng transaction, rollback chung nếu có lỗi ở trên)
        if (voucher != null) {
            voucher.setQuantity(voucher.getQuantity() - 1);
            voucherRepository.save(voucher);
        }

        return savedOrder;
    }

    // Cập nhật trạng thái giao hàng cho 1 đơn POS có giao tận nơi
    @Transactional
    public void updateShippingStatus(Integer posOrderId, String status) {
        PosOrder order = posOrderRepository.findById(posOrderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng."));

        if (!Boolean.TRUE.equals(order.getIsShipping())) {
            throw new RuntimeException("Đơn này không phải đơn giao hàng tận nơi.");
        }

        order.setShippingStatus(status);
        posOrderRepository.save(order);
    }

    // Lấy TOÀN BỘ đơn POS cho trang lịch sử — dùng query JOIN FETCH để tránh lazy-loading
    // exception / thiếu dữ liệu khi render ngoài transaction, và tránh N+1 query.
    @Transactional(readOnly = true)
    public List<PosOrder> getAllPosOrders() {
        return posOrderRepository.findAllWithDetailsOrderByOrderDateDesc();
    }

    // Tính tổng doanh thu từ 1 danh sách đơn POS — dùng cho KPI ở trang lịch sử,
    // tính sẵn ở Java thay vì dùng expression phức tạp trong Thymeleaf.
    public BigDecimal getTotalRevenue(List<PosOrder> orders) {
        if (orders == null) return BigDecimal.ZERO;
        return orders.stream()
                .map(o -> o.getTotalAmount() == null ? BigDecimal.ZERO : o.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}