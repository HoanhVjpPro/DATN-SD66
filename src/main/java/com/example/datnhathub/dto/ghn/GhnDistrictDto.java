package com.example.datnhathub.dto.ghn;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GhnDistrictDto {
    @JsonProperty("DistrictID")
    private Integer districtId;

    @JsonProperty("ProvinceID")
    private Integer provinceId;

    @JsonProperty("DistrictName")
    private String districtName;
}