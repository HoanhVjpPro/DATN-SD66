package com.example.datnhathub.controller;

import com.example.datnhathub.dto.ghn.*;
import com.example.datnhathub.service.GhnShippingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shipping")
public class ShippingApiController {

    @Autowired
    private GhnShippingService ghnShippingService;

    // Phí cố định dự phòng nếu GHN sandbox lỗi/timeout — khớp phí GHN hiển thị cũ trong hệ thống
    private static final BigDecimal FALLBACK_FEE = new BigDecimal("30000");

    @GetMapping("/ghn/provinces")
    public List<GhnProvinceDto> provinces() {
        return ghnShippingService.getProvinces();
    }

    @GetMapping("/ghn/districts")
    public List<GhnDistrictDto> districts(@RequestParam Integer provinceId) {
        return ghnShippingService.getDistricts(provinceId);
    }

    @GetMapping("/ghn/wards")
    public List<GhnWardDto> wards(@RequestParam Integer districtId) {
        return ghnShippingService.getWards(districtId);
    }

    // weightGram tính từ tổng số lượng sản phẩm trong giỏ (xem mục 5)
    @GetMapping("/ghn/fee")
    public ResponseEntity<?> fee(@RequestParam Integer districtId,
                                 @RequestParam String wardCode,
                                 @RequestParam(defaultValue = "300") int weightGram) {
        Map<String, Object> result = new HashMap<>();
        try {
            BigDecimal fee = ghnShippingService.calculateFee(districtId, wardCode, weightGram);
            result.put("success", true);
            result.put("fee", fee);
            result.put("fallback", false);
        } catch (GhnShippingService.GhnUnavailableException e) {
            // Fallback — không chặn checkout khi GHN sandbox sập/timeout
            result.put("success", true);
            result.put("fee", FALLBACK_FEE);
            result.put("fallback", true);
            result.put("message", "Không kết nối được GHN, tạm dùng phí ước tính.");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
}