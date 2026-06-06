package com.example.demo.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class QuickAddProductRequest {
    private String name;
    private String sku;
    private String unit;
    private BigDecimal basePrice;
    private BigDecimal salePrice;
    private Integer categoryId;
    private Boolean isIngredient;
    private Boolean active;
    private String imageUrl;
}
