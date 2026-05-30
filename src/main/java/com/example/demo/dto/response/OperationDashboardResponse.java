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
public class OperationDashboardResponse {
    private OperationMetrics operationMetrics;
    private List<PeakHourDto> peakHoursChart;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OperationMetrics {
        private Integer avgPreparationTimeSeconds; 
        private Double onTimeRate;                 
        private Integer defectItemsCount;          
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PeakHourDto {
        private String hour;
        private Integer totalOrders;
    }
}