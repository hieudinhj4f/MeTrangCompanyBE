package com.example.demo.controller;

import com.example.demo.dto.response.CustomerProfileResponse;
import com.example.demo.entity.Customer;
import com.example.demo.security.JwtAuthFilter;
import com.example.demo.service.CustomerService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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

    @GetMapping("/search")
    public ResponseEntity<?> searchCustomerByKeyword(
            @RequestParam String keyword, 
            HttpServletRequest request) {
        try {
            
            Customer customer = customerService.searchCustomerForPOS(keyword);
            return ResponseEntity.ok(customer);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("reason", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("reason", "Lỗi xử lý hệ thống"));
        }
    }

    @GetMapping("/b2b")
    public ResponseEntity<?> getEnterprisePartners(HttpServletRequest request) {
        try {
            assertAdminAccess(request); // Chặn khách thường truy cập
            List<Customer> partners = customerService.getAllEnterprisePartners();
            return ResponseEntity.ok(partners);
        } catch (Exception e) {
            return ResponseEntity.status(403).body(Map.of("reason", e.getMessage()));
        }
    }

    @PostMapping("/b2b")
    public ResponseEntity<?> createEnterprisePartner(@RequestBody Customer requestData, HttpServletRequest request) {
        try {
            assertAdminAccess(request); // Phân quyền bảo mật
            Customer saved = customerService.createEnterprisePartner(requestData);
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("reason", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("reason", "Lỗi máy chủ: " + e.getMessage()));
        }
    }

    @PutMapping("/b2b/{id}")
    public ResponseEntity<?> updateEnterprisePartner(
            @PathVariable UUID id,
            @RequestBody Customer updateData,
            HttpServletRequest request) {
        try {
            assertAdminAccess(request); 
            Customer updated = customerService.updateEnterprisePartner(id, updateData);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("reason", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("reason", "Lỗi xử lý: " + e.getMessage()));
        }
    }

    @GetMapping("/enterprise/workers")
    public ResponseEntity<?> getEnterpriseWorkers(HttpServletRequest request) {
        try {
            String role = (String) request.getAttribute(JwtAuthFilter.ATTR_ROLE);
            if (!"ENTERPRISE".equals(role)) {
                return ResponseEntity.status(403).body(Map.of("reason", "Chỉ doanh nghiệp mới được xem danh sách này."));
            }
            UUID enterpriseId = resolveCustomerId(request);
            List<Customer> workers = customerService.getWorkersByEnterprise(enterpriseId);
            return ResponseEntity.ok(workers);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("reason", "Lỗi xử lý: " + e.getMessage()));
        }
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

    private void assertAdminAccess(HttpServletRequest request) {
        String role = (String) request.getAttribute(JwtAuthFilter.ATTR_ROLE);
        if (!"ADMIN".equals(role)) {
            throw new RuntimeException("Truy cập bị từ chối! Chỉ Quản trị viên mới được thao tác Hồ sơ B2B.");
        }
    }
}