package com.example.datnhathub.controller;

import com.example.datnhathub.repository.OrderRepository;
import com.example.datnhathub.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAdvice {

    @Autowired
    private OrderService orderRepository;   // hoặc OrderService
//    @ModelAttribute("pendingReturns")
//    public long pendingReturns() {
//        return orderRepository.countByReturnStatus("Chờ xác nhận");
//    }
}