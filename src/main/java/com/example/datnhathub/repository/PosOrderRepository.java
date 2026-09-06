package com.example.datnhathub.repository;

import com.example.datnhathub.entity.PosOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PosOrderRepository extends JpaRepository<PosOrder, Integer> {

    List<PosOrder> findAllByOrderByOrderDateDesc();

    // Lấy TOÀN BỘ đơn POS, JOIN FETCH sẵn posCustomer + employee.user + details.productDetail.product
    @Query("""
        SELECT DISTINCT o FROM PosOrder o
        LEFT JOIN FETCH o.posCustomer
        LEFT JOIN FETCH o.employee e
        LEFT JOIN FETCH e.user
        LEFT JOIN FETCH o.details d
        LEFT JOIN FETCH d.productDetail pd
        LEFT JOIN FETCH pd.product
        ORDER BY o.orderDate DESC
    """)
    List<PosOrder> findAllWithDetailsOrderByOrderDateDesc();
}