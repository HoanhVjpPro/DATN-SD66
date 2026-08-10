package com.example.datnhathub.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public interface ReviewDTO {
    public String getCustomerName();
    public Integer getRating();
    String getComment();
    String getAdminReply();
}
