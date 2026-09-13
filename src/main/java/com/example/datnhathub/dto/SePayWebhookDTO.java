package com.example.datnhathub.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SePayWebhookDTO {
    private Long id;
    private String gateway;
    private String transactionDate;
    private String accountNo;
    private String code;
    private String content;
    private String transferType;
    private BigDecimal transferAmount; // Sinh ra getTransferAmount()
    private BigDecimal accumulated;
    private String subAccount;
    private String referenceCode;     // Sinh ra getReferenceCode()
    private String description;
}