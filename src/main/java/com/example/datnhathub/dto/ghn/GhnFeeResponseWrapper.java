package com.example.datnhathub.dto.ghn;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GhnFeeResponseWrapper {
    private Integer code;
    private String message;
    private GhnFeeData data;

    @Getter
    @Setter
    public static class GhnFeeData {
        private Integer total; // tổng phí ship (đồng)
    }
}