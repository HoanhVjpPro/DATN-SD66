package com.example.datnhathub.scheduler;

import com.example.datnhathub.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OrderCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrderCleanupScheduler.class);

    @Autowired
    private OrderService orderService;

    // (fixedRate tính bằng millisecond: 60_000 = 1 phút)
    @Scheduled(fixedRate = 60_000)
    public void cancelExpiredUnpaidOrders() {
        int count = orderService.cancelExpiredUnpaidOrders();
        if (count > 0) {
            log.info("Đã tự động hủy {} đơn hàng quá hạn thanh toán (>15 phút).", count);
        }
    }
}