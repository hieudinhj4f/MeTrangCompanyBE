package com.example.demo.dto.response;

import com.example.demo.entity.Category;
import com.example.demo.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {

    private Long id;
    private String sku;
    private String name;
    private BigDecimal basePrice;
    private String unit;
    private Boolean isIngredient;
    private Boolean active;
    private Integer categoryId;
    private String categoryName;
    private String imageUrl;

    public static ProductResponse from(Product product) {
        if (product == null) {
            return null;
        }
        Category category = product.getCategory();
        return ProductResponse.builder()
                .id(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .basePrice(product.getBasePrice())
                .unit(product.getUnit())
                .isIngredient(product.getIsIngredient())
                .active(product.getActive())
                .categoryId(category != null ? category.getId() : null)
                .categoryName(category != null ? category.getCategoryName() : null)
                .imageUrl(product.getImageUrl())
                .build();
    }
}
