package com.example.demo.dto.response;

import com.example.demo.entity.Inventories;
import com.example.demo.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryItemResponse {

    private InventoryKey id;
    private Integer quantity;
    private LocalDate expiryDate;
    private ProductSummary product;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InventoryKey {
        private Integer warehouseId;
        private Long productId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductSummary {
        private Long id;
        private String sku;
        private String name;
        private String unit;
        private Boolean isIngredient;
    }

    public static InventoryItemResponse from(Inventories inventory) {
        if (inventory == null || inventory.getId() == null) {
            return null;
        }
        Product p = inventory.getProduct();
        return InventoryItemResponse.builder()
                .id(InventoryKey.builder()
                        .warehouseId(inventory.getId().getWarehouseId())
                        .productId(inventory.getId().getProductId())
                        .build())
                .quantity(inventory.getQuantity())
                .product(p == null ? null : ProductSummary.builder()
                        .id(p.getId())
                        .sku(p.getSku())
                        .name(p.getName())
                        .unit(p.getUnit())
                        .isIngredient(p.getIsIngredient())
                        .build())
                .build();
    }
}
