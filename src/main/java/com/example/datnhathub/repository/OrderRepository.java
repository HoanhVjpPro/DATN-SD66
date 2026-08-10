package com.example.datnhathub.repository;

import com.example.datnhathub.entity.Orders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Orders, Integer> {
    List<Orders> findByCustomerCustomerIdOrderByOrderDateDesc(Integer customerId);
    List<Orders> findAllByOrderByOrderDateDesc();
    long countByStatus(String status);

    // Đơn "Chờ thanh toán" quá hạn (đặt trước thời điểm truyền vào) -> dùng cho job tự hủy
    List<Orders> findByStatusAndOrderDateBefore(String status, java.time.LocalDateTime cutoff);

    List<Orders> findByStatusAndShippingShipperIsNull(String status);
    List<Orders> findByShippingShipperShipperIdOrderByOrderDateDesc(Integer shipperId);

    // Lấy tất cả đơn có yêu cầu trả hàng
    List<Orders> findByReturnStatusNotNull();

    // Lấy theo trạng thái trả hàng
    List<Orders> findByReturnStatusOrderByReturnDateDesc(String returnStatus);

    // Doanh thu theo tháng (6 tháng gần nhất), bỏ qua đơn đã hủy và đơn đã trả hàng
    @Query(value = "SELECT FORMAT(o.OrderDate, 'yyyy-MM') AS month, SUM(o.TotalAmount) AS revenue " +
            "FROM Orders o " +
            "LEFT JOIN Shipping s ON o.OrderID = s.OrderID " +
            "WHERE (o.Status = N'Hoàn thành' OR (o.Status = N'Đang giao' AND s.ConfirmedByShipper = 1)) " +
            "AND (o.ReturnStatus IS NULL OR o.ReturnStatus <> N'Đã chấp nhận') " +
            "AND o.OrderDate >= DATEADD(MONTH, -5, GETDATE()) " +
            "GROUP BY FORMAT(o.OrderDate, 'yyyy-MM') " +
            "ORDER BY month", nativeQuery = true)
    List<MonthlyRevenueProjection> getMonthlyRevenue();

    // Top 5 sản phẩm bán chạy nhất theo số lượng, bỏ qua đơn đã hủy và đơn đã trả hàng
    @Query(value = "SELECT TOP 5 p.ProductName AS productName, " +
            "SUM(od.Quantity) AS totalQty, " +
            "SUM(od.Quantity * od.UnitPrice) AS totalRevenue " +
            "FROM Order_Detail od " +
            "JOIN Product_Detail pd ON od.ProductDetailID = pd.ProductDetailID " +
            "JOIN Product p ON pd.ProductID = p.ProductID " +
            "JOIN Orders o ON od.OrderID = o.OrderID " +
            "LEFT JOIN Shipping s ON o.OrderID = s.OrderID " +
            "WHERE (o.Status = N'Hoàn thành' OR (o.Status = N'Đang giao' AND s.ConfirmedByShipper = 1)) " +
            "AND (o.ReturnStatus IS NULL OR o.ReturnStatus <> N'Đã chấp nhận') " +
            "GROUP BY p.ProductName " +
            "ORDER BY totalQty DESC", nativeQuery = true)
    List<TopProductProjection> getTopSellingProducts();

    interface MonthlyRevenueProjection {
        String getMonth();
        java.math.BigDecimal getRevenue();
    }

    interface TopProductProjection {
        String getProductName();
        Long getTotalQty();
        java.math.BigDecimal getTotalRevenue();
    }

    // Top N sản phẩm bán chạy (theo productId, dùng cho trang chủ), cũng loại đơn trả hàng
    @Query(value = "SELECT TOP (:limit) p.ProductID AS productId, SUM(od.Quantity) AS totalQty " +
            "FROM Order_Detail od " +
            "JOIN Product_Detail pd ON od.ProductDetailID = pd.ProductDetailID " +
            "JOIN Product p ON pd.ProductID = p.ProductID " +
            "JOIN Orders o ON od.OrderID = o.OrderID " +
            "LEFT JOIN Shipping s ON o.OrderID = s.OrderID " +
            "WHERE (o.Status = N'Hoàn thành' OR (o.Status = N'Đang giao' AND s.ConfirmedByShipper = 1)) " +
            "AND (o.ReturnStatus IS NULL OR o.ReturnStatus <> N'Đã chấp nhận') " +
            "GROUP BY p.ProductID " +
            "ORDER BY totalQty DESC", nativeQuery = true)
    List<BestSellingProjection> findBestSellingProductIds(@Param("limit") int limit);

    interface BestSellingProjection {
        Integer getProductId();

        Long getTotalQty();
    }

    // Toàn bộ sản phẩm đã bán, sắp theo số lượng giảm dần (dùng cho trang xem chi tiết doanh số)
    @Query(value = "SELECT p.ProductName AS productName, " +
            "SUM(od.Quantity) AS totalQty, " +
            "SUM(od.Quantity * od.UnitPrice) AS totalRevenue " +
            "FROM Order_Detail od " +
            "JOIN Product_Detail pd ON od.ProductDetailID = pd.ProductDetailID " +
            "JOIN Product p ON pd.ProductID = p.ProductID " +
            "JOIN Orders o ON od.OrderID = o.OrderID " +
            "LEFT JOIN Shipping s ON o.OrderID = s.OrderID " +
            "WHERE (o.Status = N'Hoàn thành' OR (o.Status = N'Đang giao' AND s.ConfirmedByShipper = 1)) " +
            "AND (o.ReturnStatus IS NULL OR o.ReturnStatus <> N'Đã chấp nhận') " +
            "GROUP BY p.ProductName " +
            "ORDER BY totalQty DESC", nativeQuery = true)
    List<TopProductProjection> getAllProductSales();

    // Doanh thu theo ngày (14 ngày gần nhất), cùng điều kiện lọc như doanh thu tháng
    @Query(value = "SELECT FORMAT(o.OrderDate, 'dd/MM') AS day, SUM(o.TotalAmount) AS revenue " +
            "FROM Orders o " +
            "LEFT JOIN Shipping s ON o.OrderID = s.OrderID " +
            "WHERE (o.Status = N'Hoàn thành' OR (o.Status = N'Đang giao' AND s.ConfirmedByShipper = 1)) " +
            "AND (o.ReturnStatus IS NULL OR o.ReturnStatus <> N'Đã chấp nhận') " +
            "AND o.OrderDate >= DATEADD(DAY, -6, CAST(GETDATE() AS DATE)) " +
            "GROUP BY FORMAT(o.OrderDate, 'dd/MM'), CAST(o.OrderDate AS DATE) " +
            "ORDER BY CAST(o.OrderDate AS DATE)", nativeQuery = true)
    List<DailyRevenueProjection> getDailyRevenue();

    // Doanh thu theo tuần (8 tuần gần nhất), nhãn dạng "Tuần N tháng M"
    @Query(value = "SELECT CONCAT(N'Tuần ', ((DAY(o.OrderDate) - 1) / 7) + 1, N' tháng ', MONTH(o.OrderDate)) AS week, " +
            "SUM(o.TotalAmount) AS revenue " +
            "FROM Orders o " +
            "LEFT JOIN Shipping s ON o.OrderID = s.OrderID " +
            "WHERE (o.Status = N'Hoàn thành' OR (o.Status = N'Đang giao' AND s.ConfirmedByShipper = 1)) " +
            "AND (o.ReturnStatus IS NULL OR o.ReturnStatus <> N'Đã chấp nhận') " +
            "AND o.OrderDate >= DATEADD(WEEK, -7, GETDATE()) " +
            "GROUP BY DATEPART(YEAR, o.OrderDate), MONTH(o.OrderDate), ((DAY(o.OrderDate) - 1) / 7) + 1 " +
            "ORDER BY DATEPART(YEAR, o.OrderDate), MONTH(o.OrderDate), ((DAY(o.OrderDate) - 1) / 7) + 1", nativeQuery = true)
    List<WeeklyRevenueProjection> getWeeklyRevenue();

    interface DailyRevenueProjection {
        String getDay();
        java.math.BigDecimal getRevenue();
    }

    interface WeeklyRevenueProjection {
        String getWeek();
        java.math.BigDecimal getRevenue();
    }
}