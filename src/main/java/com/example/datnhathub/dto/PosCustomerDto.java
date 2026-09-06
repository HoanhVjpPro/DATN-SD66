package com.example.datnhathub.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PosCustomerDto {
    private Integer posCustomerId;
    private String fullName;
    private String phone;
    private String email;
    private String address;
}