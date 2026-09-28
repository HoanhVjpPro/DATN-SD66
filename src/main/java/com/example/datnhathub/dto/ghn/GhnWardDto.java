package com.example.datnhathub.dto.ghn;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GhnWardDto {
    @JsonProperty("WardCode")
    private String wardCode;

    @JsonProperty("DistrictID")
    private Integer districtId;

    @JsonProperty("WardName")
    private String wardName;
}