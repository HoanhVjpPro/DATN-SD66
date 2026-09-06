package com.example.datnhathub.controller;

import com.example.datnhathub.service.OrderService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

    private final OrderService orderService;

    public GlobalControllerAdvice(OrderService orderService) {
        this.orderService = orderService;
    }

    @ModelAttribute("pendingOrders")
    public Long getPendingOrders() {
        return orderService.countByStatus("Chờ xác nhận");
    }
}