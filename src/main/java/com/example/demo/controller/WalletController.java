package com.example.demo.controller;

import com.example.demo.entity.TransactionHistory;
import com.example.demo.entity.Wallet;
import com.example.demo.security.JwtAuthFilter;
import com.example.demo.service.CustomerService;
import com.example.demo.service.WalletService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class WalletController {

    private final WalletService walletService;
    private final CustomerService customerService;

    @GetMapping("/me")
    public ResponseEntity<?> getMyBalance(HttpServletRequest request) {
        UUID customerId = requireCustomerId(request);
        return getBalance(customerId, request);
    }

    @GetMapping("/balance/{customerId}")
    public ResponseEntity<?> getBalance(
            @PathVariable UUID customerId,
            HttpServletRequest request) {
        try {
            assertCustomerAccess(customerId, request);
            Wallet wallet = walletService.getWalletByCustomerId(customerId);

            return ResponseEntity.ok(Map.of(
                    "status", "Thành công",
                    "customerId", customerId,
                    "fullName", wallet.getCustomer().getFullName(),
                    "balance", wallet.getBalance()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(Map.of(
                    "status", "Thất bại",
                    "reason", e.getMessage()
            ));
        }
    }

    @PostMapping("/topup")
    public ResponseEntity<?> topUp(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        try {
            UUID customerOrUserId = resolveTargetId(body, request);
            BigDecimal amount = new BigDecimal(body.get("amount"));
            UUID performedBy = (UUID) request.getAttribute(JwtAuthFilter.ATTR_USER_ID);

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Số tiền nạp phải lớn hơn 0");
            }

            Wallet updatedWallet = walletService.depositMoney(customerOrUserId, amount, performedBy);

            return ResponseEntity.ok(Map.of(
                    "status", "Nạp tiền thành công",
                    "customerId", updatedWallet.getCustomer().getId(),
                    "fullName", updatedWallet.getCustomer().getFullName(),
                    "newBalance", updatedWallet.getBalance()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "Thất bại",
                    "reason", e.getMessage()
            ));
        }
    }

    @GetMapping("/history/me")
    public ResponseEntity<?> getMyHistory(HttpServletRequest request) {
        UUID customerId = requireCustomerId(request);
        return getHistory(customerId, request);
    }

    @GetMapping("/history/{customerId}")
    public ResponseEntity<?> getHistory(
            @PathVariable UUID customerId,
            HttpServletRequest request) {
        try {
            assertCustomerAccess(customerId, request);
            List<TransactionHistory> history = walletService.getTransactionHistory(customerId);
            return ResponseEntity.ok(Map.of(
                    "status", "Thành công",
                    "totalTransactions", history.size(),
                    "data", history
            ));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of(
                    "status", "Thất bại",
                    "reason", e.getMessage()
            ));
        }
    }

    @PostMapping("/topup/bulk")
    public ResponseEntity<?> topUpBulk(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        try {
            List<String> idStrings = (List<String>) body.get("userIds");
            if (idStrings == null || idStrings.isEmpty()) {
                throw new IllegalArgumentException("Danh sách người nhận trống");
            }
            List<UUID> targetIds = idStrings.stream().map(UUID::fromString).toList();
            BigDecimal amount = new BigDecimal(body.get("amount").toString());
            UUID performedBy = (UUID) request.getAttribute(JwtAuthFilter.ATTR_USER_ID);

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Số tiền nạp phải lớn hơn 0");
            }

            List<Wallet> updatedWallets = walletService.depositMoneyBulk(targetIds, amount, performedBy);

            return ResponseEntity.ok(Map.of(
                    "status", "Thành công",
                    "message", "Nạp tiền hàng loạt hoàn tất",
                    "totalProcessed", updatedWallets.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "Thất bại",
                    "reason", e.getMessage()
            ));
        }
    }

    @GetMapping("/history/enterprise")
    public ResponseEntity<?> getEnterpriseHistory(HttpServletRequest request) {
        try {
            UUID performedBy = (UUID) request.getAttribute(JwtAuthFilter.ATTR_USER_ID);
            if (performedBy == null) throw new RuntimeException("Không tìm thấy thông tin định danh");
            List<TransactionHistory> history = walletService.getHistoryByPerformedBy(performedBy);
            return ResponseEntity.ok(Map.of(
                    "status", "Thành công",
                    "totalTransactions", history.size(),
                    "data", history
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "Thất bại", "reason", e.getMessage()));
        }
    }

    private UUID requireCustomerId(HttpServletRequest request) {
        UUID authCustomerId = (UUID) request.getAttribute(JwtAuthFilter.ATTR_CUSTOMER_ID);
        if (authCustomerId == null) {
            UUID userId = (UUID) request.getAttribute(JwtAuthFilter.ATTR_USER_ID);
            if (userId != null) {
                return customerService.ensureCustomerForUserId(userId);
            }
            throw new RuntimeException("Không xác định được khách hàng từ token");
        }
        return authCustomerId;
    }

    private UUID resolveTargetId(Map<String, String> body, HttpServletRequest request) {
        UUID authCustomerId = (UUID) request.getAttribute(JwtAuthFilter.ATTR_CUSTOMER_ID);
        if (authCustomerId != null) {
            return authCustomerId;
        }
        String customerId = body.get("customerId");
        if (customerId != null && !customerId.isBlank()) {
            return UUID.fromString(customerId);
        }
        String userId = body.get("userId");
        if (userId != null && !userId.isBlank()) {
            return customerService.ensureCustomerForUserId(UUID.fromString(userId));
        }
        UUID authUserId = (UUID) request.getAttribute(JwtAuthFilter.ATTR_USER_ID);
        if (authUserId != null) {
            return customerService.ensureCustomerForUserId(authUserId);
        }
        throw new IllegalArgumentException("Thiếu customerId hoặc userId");
    }

    private void assertCustomerAccess(UUID customerId, HttpServletRequest request) {
        UUID authCustomerId = (UUID) request.getAttribute(JwtAuthFilter.ATTR_CUSTOMER_ID);
        String role = (String) request.getAttribute(JwtAuthFilter.ATTR_ROLE);
        if ("ADMIN".equals(role)) {
            return;
        }
        if (authCustomerId != null && !authCustomerId.equals(customerId)) {
            throw new RuntimeException("Không có quyền truy cập ví này");
        }
    }
}
