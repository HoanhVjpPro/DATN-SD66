package com.example.datnhathub.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Entity
@Table(name = "POS_Order")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PosOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PosOrderID")
    private Integer posOrderId;

    @ManyToOne
    @JoinColumn(name = "PosCustomerID", nullable = false)
    private PosCustomer posCustomer;

    @ManyToOne
    @JoinColumn(name = "EmployeeID", nullable = false)
    private Employee employee;

    @Column(name = "OrderDate", nullable = false)
    private LocalDateTime orderDate = LocalDateTime.now();

    // TotalAmount = Tạm tính - Giảm giá (voucher) + Phí vận chuyển (nếu có ship)
    @Column(name = "TotalAmount", precision = 18, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    // 'CASH' / 'CARD' / 'TRANSFER'
    @Column(name = "PaymentMethod", length = 20, nullable = false)
    private String paymentMethod;

    @Column(name = "Status", length = 20, nullable = false)
    private String status = "Hoàn thành";

    // Voucher đã áp dụng cho đơn (có thể null nếu không dùng)
    @ManyToOne
    @JoinColumn(name = "VoucherID")
    private Voucher voucher;

    // Số tiền đã giảm nhờ voucher — lưu lại để hiển thị hóa đơn/lịch sử
    @Column(name = "DiscountAmount", precision = 18, scale = 2)
    private BigDecimal discountAmount;

    // ── Giao hàng tận nơi (nội bộ, KHÔNG tích hợp API hãng vận chuyển) ──
    @Column(name = "IsShipping", nullable = false)
    private Boolean isShipping = false;

    @Column(name = "ShippingAddress", length = 255)
    private String shippingAddress;

    // Đơn vị vận chuyển được chọn tại quầy: 'GHTK' / 'GHN' — quyết định luôn ShippingFee (fixed theo đơn vị)
    @Column(name = "ShippingProvider", length = 20)
    private String shippingProvider;

    @Column(name = "ShippingFee", precision = 18, scale = 2)
    private BigDecimal shippingFee;

    // 'Chưa giao' / 'Đang giao' / 'Đã giao' — null nếu isShipping = false
    @Column(name = "ShippingStatus", length = 30)
    private String shippingStatus;

    @OneToMany(mappedBy = "posOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PosOrderDetail> details;

    // Mã đơn hiển thị cho khách, VD: POS20260819-15
    public String getOrderCode() {
        String datePart = orderDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "POS" + datePart + "-" + posOrderId;
    }

    public Integer getTotalQuantity() {
        if (details == null) return 0;
        return details.stream().mapToInt(PosOrderDetail::getQuantity).sum();
    }

    // Tạm tính trước khi trừ voucher và cộng phí ship
    // subtotal = totalAmount - shippingFee + discountAmount
    public BigDecimal getSubtotal() {
        BigDecimal discount = discountAmount == null ? BigDecimal.ZERO : discountAmount;
        BigDecimal ship = shippingFee == null ? BigDecimal.ZERO : shippingFee;
        BigDecimal total = totalAmount == null ? BigDecimal.ZERO : totalAmount;
        return total.subtract(ship).add(discount);
    }
}