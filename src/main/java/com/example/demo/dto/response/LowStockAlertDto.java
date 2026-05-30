package com.example.demo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LowStockAlertDto {
    private Long ingredientId;
    private String name;
    private Double currentStock;
    private Double minStockLevel;
    private String unit;
    private String status;
}