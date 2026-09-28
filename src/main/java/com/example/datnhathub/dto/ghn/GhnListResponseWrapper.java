package com.example.datnhathub.dto.ghn;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GhnListResponseWrapper<T> {
    private Integer code;
    private String message;
    private List<T> data;
}