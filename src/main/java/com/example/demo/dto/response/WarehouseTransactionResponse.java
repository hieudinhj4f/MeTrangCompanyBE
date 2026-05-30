package com.example.demo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseTransactionResponse {
    private LocalDateTime createdAt;
    private String type; 
    private String productName;
    private String productSku;
    private Integer quantity;
    private String staffName;
}
