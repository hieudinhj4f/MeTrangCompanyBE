package com.example.demo.dto.request;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PriceConfigRequest {
    private BigDecimal price;
    private String priceType; 
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String description;
}