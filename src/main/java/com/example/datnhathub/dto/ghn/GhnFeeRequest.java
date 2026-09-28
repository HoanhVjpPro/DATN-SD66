package com.example.datnhathub.dto.ghn;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GhnFeeRequest {
    @JsonProperty("service_type_id")
    private Integer serviceTypeId; // 2 = Hàng nhẹ (chuẩn), lấy từ available-services

    @JsonProperty("from_district_id")
    private Integer fromDistrictId;

    @JsonProperty("from_ward_code")
    private String fromWardCode;

    @JsonProperty("to_district_id")
    private Integer toDistrictId;

    @JsonProperty("to_ward_code")
    private String toWardCode;

    private Integer weight = 300;      // gram — ước lượng, xem mục 5
    private Integer length = 20;       // cm
    private Integer width = 20;
    private Integer height = 10;

    @JsonProperty("insurance_value")
    private Integer insuranceValue = 0; // giá trị khai giá (0 = không khai giá)
}