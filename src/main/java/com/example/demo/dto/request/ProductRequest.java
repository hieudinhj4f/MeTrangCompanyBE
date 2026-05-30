package com.example.demo.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {
    private String name;
    private String sku;
    private Double basePrice;
    private Double salePrice;
    private Boolean isIngredient;
    private Integer categoryId;
    private String description;
    private String imageUrl;
}