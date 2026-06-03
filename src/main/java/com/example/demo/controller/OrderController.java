package com.example.demo.controller;

import com.example.demo.dto.request.OrderRequest;
import com.example.demo.dto.request.UpdateOrderStatusRequest;
import com.example.demo.dto.response.OrderResponse;
import com.example.demo.entity.Order;
import com.example.demo.security.JwtAuthFilter;
import com.example.demo.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/place")
    public ResponseEntity<?> placeOrder(
            @RequestBody OrderRequest request,
            HttpServletRequest httpRequest) {
        try {
            UUID customerId = resolveCustomerId(request, httpRequest);
            Order order = orderService.placeOrder(
                    customerId,
                    request.getWarehouseId(),
                    request.getItems(),
                    Order.PaymentMethod.valueOf(request.getPaymentMethod())
            );

            return ResponseEntity.ok(Map.of(
                    "status", "Thành công",
                    "orderId", order.getId(),
                    "totalAmount", order.getTotalAmount()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("reason", e.getMessage()));
        }
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable UUID orderId) {
        try {
            orderService.cancelOrder(orderId);
            return ResponseEntity.ok(Map.of("message", "Hủy đơn hàng và hoàn tiền thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("reason", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllOrders(HttpServletRequest httpRequest) {
        try {
            if (!isAuthenticated(httpRequest)) {
                return ResponseEntity.status(401).body(Map.of("reason", "Vui lòng đăng nhập"));
            }
            List<OrderResponse> orders = orderService.getAllOrders();
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("reason", e.getMessage()));
        }
    }

    @GetMapping("/my-orders")
    public ResponseEntity<?> getMyOrders(HttpServletRequest httpRequest) {
        try {
            UUID customerId = (UUID) httpRequest.getAttribute(JwtAuthFilter.ATTR_CUSTOMER_ID);
            if (customerId == null) {
                return ResponseEntity.status(401).body(Map.of("reason", "Vui lòng đăng nhập lại!"));
            }
            List<OrderResponse> myOrders = orderService.getOrdersByCustomerId(customerId);
            return ResponseEntity.ok(myOrders);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("reason", e.getMessage()));
        }
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable UUID orderId,
            @RequestBody UpdateOrderStatusRequest request) {
        try {
            if (request == null || request.getStatus() == null || request.getStatus().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("reason", "Trạng thái không được để trống"));
            }
            OrderResponse updated = orderService.updateOrderStatus(orderId, request.getStatus());
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("reason", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("reason", e.getMessage()));
        }
    }

    /** Fallback for clients that cannot send PATCH */
    @PutMapping("/{orderId}/status")
    public ResponseEntity<?> updateOrderStatusPut(
            @PathVariable UUID orderId,
            @RequestBody UpdateOrderStatusRequest request) {
        return updateOrderStatus(orderId, request);
    }

    private boolean isAuthenticated(HttpServletRequest httpRequest) {
        return httpRequest.getAttribute(JwtAuthFilter.ATTR_USER_ID) != null
                || httpRequest.getAttribute(JwtAuthFilter.ATTR_CUSTOMER_ID) != null;
    }

    private UUID resolveCustomerId(OrderRequest request, HttpServletRequest httpRequest) {
        UUID authCustomerId = (UUID) httpRequest.getAttribute(JwtAuthFilter.ATTR_CUSTOMER_ID);
        if (authCustomerId != null) {
            return authCustomerId;
        }
        if (request.getCustomerId() != null) {
            return request.getCustomerId();
        }
        UUID authUserId = (UUID) httpRequest.getAttribute(JwtAuthFilter.ATTR_USER_ID);
        if (authUserId != null) {
            return authUserId;
        }
        throw new IllegalArgumentException("Thiếu customerId");
    }
}
