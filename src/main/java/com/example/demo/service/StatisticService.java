package com.example.demo.service;

import com.example.demo.dto.response.*;
import com.example.demo.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime; 
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticService {

    private final OrderRepository orderRepository;

    // =========================================================
    // TAB 1: TỔNG HỢP DỮ LIỆU TÀI CHÍNH (FINANCE DASHBOARD)
    // =========================================================
    public FinanceDashboardResponse getFinanceDashboard(LocalDateTime startDate, LocalDateTime endDate) {
        // 1. Lấy dữ liệu biểu đồ (từ hàm Helper bên dưới)
        List<RevenueChartDto> chartData = getRevenueChartData(startDate, endDate);

        // 2. Lấy nguyên liệu thô từ DB
        BigDecimal totalRev = orderRepository.getTotalRevenue(startDate, endDate);
        if (totalRev == null) totalRev = BigDecimal.ZERO;
        
        Integer totalOrders = orderRepository.countTotalOrders(startDate, endDate);
        if (totalOrders == null || totalOrders == 0) totalOrders = 1; // Tránh lỗi chia cho 0
        
        Integer canceledOrders = orderRepository.countCanceledOrders(startDate, endDate);
        if (canceledOrders == null) canceledOrders = 0;

        // 3. Tính toán các chỉ số phái sinh
        BigDecimal aov = totalRev.divide(new BigDecimal(totalOrders), 0, RoundingMode.HALF_UP);
        Double cancelRate = (canceledOrders.doubleValue() / totalOrders) * 100;
        BigDecimal grossProfit = totalRev.multiply(new BigDecimal("0.6")); // Giả định LN 60%

        FinanceSummaryDto summary = FinanceSummaryDto.builder()
                .totalRevenue(totalRev)
                .grossProfit(grossProfit)
                .profitMargin(60.0)
                .averageOrderValue(aov)
                .cancelRate(Math.round(cancelRate * 10.0) / 10.0)
                .canceledOrdersCount(canceledOrders)
                .revenueTrend(15.0)
                .build();

        // 4. Mock data cho phương thức thanh toán
        List<PaymentMethodDto> payments = List.of(
                PaymentMethodDto.builder()
                        .methodName("Chuyển khoản / QR")
                        .percentage(65.0)
                        .totalAmount(totalRev.multiply(new BigDecimal("0.65")))
                        .build(),
                        
                PaymentMethodDto.builder()
                        .methodName("Tiền mặt")
                        .percentage(35.0)
                        .totalAmount(totalRev.multiply(new BigDecimal("0.35")))
                        .build()
        );

        return FinanceDashboardResponse.builder()
                .summaryMetrics(summary)
                .revenueChart(chartData)
                .paymentMethods(payments)
                .build();
    }

    // =========================================================
    // TAB 2: TỔNG HỢP DỮ LIỆU VẬN HÀNH (OPERATIONS DASHBOARD)
    // =========================================================
    public OperationDashboardResponse getOperationDashboard(LocalDateTime startDate, LocalDateTime endDate) {
        List<Object[]> rawPeakData = orderRepository.getPeakHoursData(startDate, endDate);
        List<OperationDashboardResponse.PeakHourDto> peakHours = new ArrayList<>();
        
        for (Object[] row : rawPeakData) {
            String hour = row[0].toString();
            Integer count = ((Number) row[1]).intValue();
            peakHours.add(new OperationDashboardResponse.PeakHourDto(hour, count));
        }

        int totalOrders = orderRepository.countTotalOrders(startDate, endDate) != null ? orderRepository.countTotalOrders(startDate, endDate) : 1;
        
        OperationDashboardResponse.OperationMetrics metrics = OperationDashboardResponse.OperationMetrics.builder()
                .avgPreparationTimeSeconds(270) 
                .onTimeRate(95.5) 
                .defectItemsCount((int) (totalOrders * 0.02)) 
                .build();

        return OperationDashboardResponse.builder()
                .operationMetrics(metrics)
                .peakHoursChart(peakHours)
                .build();
    }

    // =========================================================
    // TAB 3: TỔNG HỢP DỮ LIỆU KHO (INVENTORY DASHBOARD)
    // =========================================================
    public InventoryDashboardResponse getInventoryDashboard() {
        List<LowStockAlertDto> alerts = List.of(
                LowStockAlertDto.builder().ingredientId(1L).name("Hạt Cafe Robusta").currentStock(2.0).minStockLevel(5.0).unit("kg").status("Nguy cấp").build(),
                LowStockAlertDto.builder().ingredientId(2L).name("Sữa tươi thanh trùng").currentStock(3.0).minStockLevel(10.0).unit("hộp").status("Nguy cấp").build()
        );

        List<InventoryDashboardResponse.IngredientEfficiencyDto> efficiencies = List.of(
                new InventoryDashboardResponse.IngredientEfficiencyDto("Hạt Cafe Robusta", 85.0),
                new InventoryDashboardResponse.IngredientEfficiencyDto("Sữa tươi thanh trùng", 60.0),
                new InventoryDashboardResponse.IngredientEfficiencyDto("Ly nhựa & Ống hút", 92.0)
        );

        return InventoryDashboardResponse.builder()
                .lowStockAlerts(alerts)
                .ingredientEfficiency(efficiencies)
                .build();
    }

    // =========================================================
    // HELPER: XỬ LÝ DỮ LIỆU BIỂU ĐỒ DOANH THU (CỦA TAB 1)
    // =========================================================
    public List<RevenueChartDto> getRevenueChartData(LocalDateTime startDate, LocalDateTime endDate) {
        List<Object[]> rawData = orderRepository.getRawDailyRevenue(startDate, endDate);
        List<RevenueChartDto> chartDataList = new ArrayList<>();

        for (Object[] row : rawData) {
            String dateLabel = row[0].toString(); 
            BigDecimal revenue = (BigDecimal) row[1];
            
            // Công thức tính Lợi nhuận gộp giả định 60%
            BigDecimal profit = revenue.multiply(new BigDecimal("0.6")); 

            // Dùng Builder để an toàn hơn việc truyền tham số vào Constructor
            RevenueChartDto dto = RevenueChartDto.builder()
                    .dateLabel(dateLabel)
                    .totalRevenue(revenue)
                    .grossProfit(profit)
                    .build();
                    
            chartDataList.add(dto);
        }

        return chartDataList;
    }
}