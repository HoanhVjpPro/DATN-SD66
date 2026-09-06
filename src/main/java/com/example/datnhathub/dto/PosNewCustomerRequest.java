package com.example.datnhathub.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PosNewCustomerRequest {
    private String phone;
    private String fullName;
    private String address;
    private String email;
}