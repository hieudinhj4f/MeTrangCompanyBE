package com.example.demo.controller;

import com.example.demo.dto.response.CustomerProfileResponse;
import com.example.demo.security.JwtAuthFilter;
import com.example.demo.service.CustomerService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile(HttpServletRequest request) {
        UUID customerId = resolveCustomerId(request);
        return customerService.getCustomerById(customerId)
                .<ResponseEntity<?>>map(c -> ResponseEntity.ok(CustomerProfileResponse.from(c)))
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of(
                        "reason", "Không tìm thấy hồ sơ khách hàng")));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCustomerById(
            @PathVariable UUID id,
            HttpServletRequest request) {
        assertCustomerAccess(id, request);
        return customerService.getCustomerById(id)
                .map(c -> ResponseEntity.<Object>ok(CustomerProfileResponse.from(c)))
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of(
                        "reason", "Không tìm thấy khách hàng với ID: " + id)));
    }

    private UUID resolveCustomerId(HttpServletRequest request) {
        UUID authCustomerId = (UUID) request.getAttribute(JwtAuthFilter.ATTR_CUSTOMER_ID);
        if (authCustomerId != null) {
            return authCustomerId;
        }
        UUID authUserId = (UUID) request.getAttribute(JwtAuthFilter.ATTR_USER_ID);
        if (authUserId != null) {
            return customerService.ensureCustomerForUserId(authUserId);
        }
        throw new IllegalArgumentException("Thiếu thông tin xác thực");
    }

    private void assertCustomerAccess(UUID customerId, HttpServletRequest request) {
        String role = (String) request.getAttribute(JwtAuthFilter.ATTR_ROLE);
        if ("ADMIN".equals(role)) {
            return;
        }
        UUID authCustomerId = (UUID) request.getAttribute(JwtAuthFilter.ATTR_CUSTOMER_ID);
        if (authCustomerId != null && authCustomerId.equals(customerId)) {
            return;
        }
        UUID authUserId = (UUID) request.getAttribute(JwtAuthFilter.ATTR_USER_ID);
        if (authUserId != null && authUserId.equals(customerId)) {
            return;
        }
        throw new RuntimeException("Không có quyền xem hồ sơ này");
    }
}
