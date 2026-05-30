package com.example.demo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinanceSummaryDto {
    private BigDecimal totalRevenue;
    private Double revenueTrend;
    private BigDecimal grossProfit;
    private Double profitMargin;
    private BigDecimal averageOrderValue;
    private Double cancelRate;
    private Integer canceledOrdersCount;
}