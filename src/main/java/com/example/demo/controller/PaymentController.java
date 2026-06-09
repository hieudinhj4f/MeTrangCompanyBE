package com.example.demo.controller;

import com.example.demo.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor // Sử dụng thay cho @Autowired để code sạch hơn
public class PaymentController {

    private final WalletService walletService;

    @PostMapping("/deposit") 
    public ResponseEntity<?> deposit(
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) UUID userId,
            @RequestParam BigDecimal amount,
            jakarta.servlet.http.HttpServletRequest request) {
        
        try {
            UUID targetId = customerId != null ? customerId : userId;
            if (targetId == null) {
                throw new IllegalArgumentException("Thiếu customerId hoặc userId");
            }
            UUID performedBy = (UUID) request.getAttribute(com.example.demo.security.JwtAuthFilter.ATTR_USER_ID);
            var wallet = walletService.depositMoney(targetId, amount, performedBy);
            
            return ResponseEntity.ok(Map.of(
                "status", "Thành công",
                "message", "Số dư ví đã được cập nhật.",
                "new_balance", wallet.getBalance()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "Thất bại",
                "message", e.getMessage()
            ));
        }
    }
}