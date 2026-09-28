package com.example.datnhathub.dto.ghn;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GhnProvinceDto {
    @JsonProperty("ProvinceID")
    private Integer provinceId;

    @JsonProperty("ProvinceName")
    private String provinceName;
}