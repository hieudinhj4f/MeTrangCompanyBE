package com.example.demo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RevenueChartDto {
    private String dateLabel;
    private LocalDate date;
    private BigDecimal totalRevenue;
    private BigDecimal grossProfit;
}