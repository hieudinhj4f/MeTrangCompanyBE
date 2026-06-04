package com.example.demo.repository;

import com.example.demo.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByWarehouse_Id(Integer warehouseId);

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product LEFT JOIN FETCH o.customer WHERE o.warehouse.id = :warehouseId")
    List<Order> findByWarehouseIdWithItems(@Param("warehouseId") Integer warehouseId);

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product LEFT JOIN FETCH o.customer LEFT JOIN FETCH o.warehouse WHERE o.customer.id = :customerId ORDER BY o.orderDate DESC")
    List<Order> findByCustomer_Id(@Param("customerId") UUID customerId);

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product LEFT JOIN FETCH o.customer LEFT JOIN FETCH o.warehouse ORDER BY o.orderDate DESC")
    List<Order> findAllWithDetails();

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product LEFT JOIN FETCH o.customer LEFT JOIN FETCH o.warehouse WHERE o.id = :id")
    Optional<Order> findByIdWithDetails(@Param("id") UUID id);

    @Query(value = "SELECT CAST(order_date AS DATE) as date_label, SUM(total_amount) as total_revenue " +
            "FROM orders " +
            "WHERE order_date BETWEEN :startDate AND :endDate " +
            "GROUP BY CAST(order_date AS DATE) " +
            "ORDER BY date_label ASC", nativeQuery = true)
    List<Object[]> getRawDailyRevenue(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query(value = "SELECT SUM(total_amount) FROM orders WHERE status != 'CANCELLED' AND order_date BETWEEN :startDate AND :endDate", nativeQuery = true)
    BigDecimal getTotalRevenue(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // 3. Tổng số đơn hàng
    @Query(value = "SELECT COUNT(id) FROM orders WHERE order_date BETWEEN :startDate AND :endDate", nativeQuery = true)
    Integer countTotalOrders(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // 4. Số đơn bị hủy
    @Query(value = "SELECT COUNT(id) FROM orders WHERE status = 'CANCELLED' AND order_date BETWEEN :startDate AND :endDate", nativeQuery = true)
    Integer countCanceledOrders(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // 5. Biểu đồ Giờ cao điểm (Peak Hours cho Tab Vận hành)
    @Query(value = "SELECT TO_CHAR(order_date, 'HH24:00') as hour_label, COUNT(id) as total_orders " +
            "FROM orders " +
            "WHERE order_date BETWEEN :startDate AND :endDate " +
            "GROUP BY TO_CHAR(order_date, 'HH24:00') " +
            "ORDER BY hour_label ASC", nativeQuery = true)
    List<Object[]> getPeakHoursData(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Tính tổng số tiền khách hàng đang nợ (Mua bằng DEBT nhưng chưa PAID)
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.customer.id = :customerId AND o.paymentMethod = 'DEBT' AND o.status != 'PAID' AND o.status != 'CANCELLED'")
    BigDecimal sumUnpaidDebtByCustomer(@Param("customerId") UUID customerId);
}
