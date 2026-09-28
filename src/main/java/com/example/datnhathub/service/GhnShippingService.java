package com.example.datnhathub.service;

import com.example.datnhathub.dto.ghn.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GhnShippingService {

    @Value("${ghn.baseUrl}")
    private String baseUrl;

    @Value("${ghn.token}")
    private String token;

    @Value("${ghn.shopId}")
    private String shopId;

    @Value("${ghn.fromDistrictId}")
    private Integer fromDistrictId;

    @Value("${ghn.fromWardCode}")
    private String fromWardCode;

    private final RestTemplate restTemplate = new RestTemplate();

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Token", token);
        headers.set("ShopId", shopId);
        return headers;
    }

    // ── Danh sách tỉnh/thành ──
    public List<GhnProvinceDto> getProvinces() {
        String url = baseUrl + "/master-data/province";
        HttpEntity<Void> entity = new HttpEntity<>(buildHeaders());

        ResponseEntity<GhnListResponseWrapper<GhnProvinceDto>> resp = restTemplate.exchange(
                url, HttpMethod.GET, entity,
                new org.springframework.core.ParameterizedTypeReference<GhnListResponseWrapper<GhnProvinceDto>>() {}
        );
        return resp.getBody() != null ? resp.getBody().getData() : List.of();
    }

    // ── Danh sách quận/huyện theo tỉnh ──
    public List<GhnDistrictDto> getDistricts(Integer provinceId) {
        String url = baseUrl + "/master-data/district";
        Map<String, Object> body = new HashMap<>();
        body.put("province_id", provinceId);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, buildHeaders());
        ResponseEntity<GhnListResponseWrapper<GhnDistrictDto>> resp = restTemplate.exchange(
                url, HttpMethod.POST, entity,
                new org.springframework.core.ParameterizedTypeReference<GhnListResponseWrapper<GhnDistrictDto>>() {}
        );
        return resp.getBody() != null ? resp.getBody().getData() : List.of();
    }

    // ── Danh sách phường/xã theo quận/huyện ──
    public List<GhnWardDto> getWards(Integer districtId) {
        String url = baseUrl + "/master-data/ward?district_id=" + districtId;
        HttpEntity<Void> entity = new HttpEntity<>(buildHeaders());

        ResponseEntity<GhnListResponseWrapper<GhnWardDto>> resp = restTemplate.exchange(
                url, HttpMethod.GET, entity,
                new org.springframework.core.ParameterizedTypeReference<GhnListResponseWrapper<GhnWardDto>>() {}
        );
        return resp.getBody() != null ? resp.getBody().getData() : List.of();
    }

    // ── Lấy service_type_id khả dụng cho tuyến from->to (GHN yêu cầu gọi trước khi tính phí) ──
    private Integer resolveServiceTypeId(Integer toDistrictId) {
        String url = baseUrl + "/v2/shipping-order/available-services";
        Map<String, Object> body = new HashMap<>();
        body.put("shop_id", Integer.parseInt(shopId));
        body.put("from_district", fromDistrictId);
        body.put("to_district", toDistrictId);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, buildHeaders());
        ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

        List<Map<String, Object>> services = (List<Map<String, Object>>) resp.getBody().get("data");
        if (services == null || services.isEmpty()) {
            throw new RuntimeException("GHN không hỗ trợ tuyến vận chuyển này.");
        }
        // Ưu tiên service_type_id = 2 (hàng nhẹ/chuẩn) nếu có, không thì lấy cái đầu tiên
        return services.stream()
                .filter(s -> Integer.valueOf(2).equals(s.get("service_type_id")))
                .findFirst()
                .map(s -> (Integer) s.get("service_type_id"))
                .orElse((Integer) services.get(0).get("service_type_id"));
    }

    // ── Tính phí ship thực tế ──
    // weightGram: cân nặng ước lượng của đơn hàng (xem mục 5 — tạm tính theo số lượng mũ)
    public BigDecimal calculateFee(Integer toDistrictId, String toWardCode, int weightGram) {
        try {
            Integer serviceTypeId = resolveServiceTypeId(toDistrictId);

            GhnFeeRequest req = new GhnFeeRequest();
            req.setServiceTypeId(serviceTypeId);
            req.setFromDistrictId(fromDistrictId);
            req.setFromWardCode(fromWardCode);
            req.setToDistrictId(toDistrictId);
            req.setToWardCode(toWardCode);
            req.setWeight(weightGram);

            String url = baseUrl + "/v2/shipping-order/fee";
            HttpEntity<GhnFeeRequest> entity = new HttpEntity<>(req, buildHeaders());

            ResponseEntity<GhnFeeResponseWrapper> resp = restTemplate.postForEntity(url, entity, GhnFeeResponseWrapper.class);

            if (resp.getBody() == null || resp.getBody().getData() == null) {
                throw new RuntimeException("GHN không trả về dữ liệu phí ship.");
            }
            return BigDecimal.valueOf(resp.getBody().getData().getTotal());

        } catch (RestClientException e) {
            // Ném lỗi riêng để Controller/Service gọi có thể fallback về phí cố định
            throw new GhnUnavailableException("Không thể lấy phí ship từ GHN: " + e.getMessage(), e);
        }
    }

    public static class GhnUnavailableException extends RuntimeException {
        public GhnUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}