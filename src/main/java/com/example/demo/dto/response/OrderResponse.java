package com.example.demo.dto.response;

import com.example.demo.entity.Order;
import com.example.demo.entity.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {

    private UUID id;
    private String status;
    private Boolean isPriority;
    private BigDecimal totalAmount;
    private LocalDateTime orderDate;
    private UUID customerId;
    private String customerName;
    private Integer warehouseId;
    private String warehouseName;
    private List<OrderItemResponse> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderItemResponse {
        private Long productId;
        private String productName;
        private String productSku;
        private Integer quantity;
        private BigDecimal priceAtPurchase;
    }

    public static OrderResponse from(Order order) {
        if (order == null) {
            return null;
        }
        return OrderResponse.builder()
                .id(order.getId())
                .status(order.getStatus() != null ? order.getStatus().name() : null)
                .isPriority(order.getIsPriority())
                .totalAmount(order.getTotalAmount())
                .orderDate(order.getOrderDate())
                .customerId(order.getCustomer() != null ? order.getCustomer().getId() : null)
                .customerName(order.getCustomer() != null ? order.getCustomer().getFullName() : "Khách lẻ")
                .warehouseId(order.getWarehouse() != null ? order.getWarehouse().getId() : null)
                .warehouseName(order.getWarehouse() != null ? order.getWarehouse().getName() : null)
                .items(order.getItems() == null ? List.of() : order.getItems().stream()
                        .map(OrderResponse::fromItem)
                        .toList())
                .build();
    }

    private static OrderItemResponse fromItem(OrderItem item) {
        if (item == null) {
            return null;
        }
        return OrderItemResponse.builder()
                .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                .productName(item.getProduct() != null ? item.getProduct().getName() : "—")
                .productSku(item.getProduct() != null ? item.getProduct().getSku() : null)
                .quantity(item.getQuantity())
                .priceAtPurchase(item.getPriceAtPurchase())
                .build();
    }
}
