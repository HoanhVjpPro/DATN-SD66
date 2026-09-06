package com.example.datnhathub.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PosCheckoutRequest {
    private Integer posCustomerId;
    private String paymentMethod;
    private String voucherCode; // tùy chọn — null hoặc rỗng nếu không dùng voucher

    // Giao hàng tận nơi (tùy chọn) — nếu isShipping = true thì shippingAddress + shippingProvider bắt buộc.
    // Phí ship KHÔNG nhận từ client — server tự tính cố định theo shippingProvider (xem PosService).
    private Boolean isShipping;
    private String shippingAddress;
    private String shippingProvider; // 'GHTK' / 'GHN'

    private List<PosCheckoutItem> items;

    @Getter
    @Setter
    public static class PosCheckoutItem {
        private Integer productDetailId;
        private Integer quantity;
    }
}