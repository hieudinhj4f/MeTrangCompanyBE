package com.example.demo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinanceDashboardResponse {
    private FinanceSummaryDto summaryMetrics;
    private List<RevenueChartDto> revenueChart;
    private List<PaymentMethodDto> paymentMethods;
}