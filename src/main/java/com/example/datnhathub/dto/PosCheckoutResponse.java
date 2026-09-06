package com.example.datnhathub.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PosCheckoutResponse {
    private Integer posOrderId;
    private String orderCode;
}