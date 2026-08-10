package com.example.datnhathub.controller;

import com.example.datnhathub.entity.Voucher;
import com.example.datnhathub.service.VoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Controller
public class VoucherApiController {

    @Autowired
    private VoucherService voucherService;

    @GetMapping("/api/vouchers/check")
    @ResponseBody
    public Map<String, Object> checkVoucher(@RequestParam String code,
                                            @RequestParam BigDecimal subtotal) {
        Map<String, Object> result = new HashMap<>();
        try {
            Voucher voucher = voucherService.validateVoucher(code);
            BigDecimal discount = voucher.calculateDiscount(subtotal);

            result.put("valid", true);
            result.put("discount", discount);
            result.put("freeShip", voucher.isFreeShip());
            result.put("code", voucher.getCode());
        } catch (Exception e) {
            result.put("valid", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}