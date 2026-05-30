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
public class InventoryDashboardResponse {
    private List<LowStockAlertDto> lowStockAlerts;
    private List<IngredientEfficiencyDto> ingredientEfficiency;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class IngredientEfficiencyDto {
        private String name;
        private Double usedPercentage;
    }
}