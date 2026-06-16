package com.example.demo.dto.request;

import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class OrderRequest {
    private UUID customerId;
    private Integer warehouseId;
    private List<OrderItemRequest> items;
    private String paymentMethod;
    private String orderType;
    private String deliveryAddress;
    private Boolean isOnlineOrder = false;

    // VAT Invoice fields
    private Boolean requiresInvoice = false;
    private String companyName;
    private String taxCode;
    private String billingAddress;
}