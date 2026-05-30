package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class TestController {

    // JdbcTemplate là công cụ của Spring giúp chạy lệnh SQL nhanh mà không cần cấu hình phức tạp
    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 1. Endpoint test kết nối BE-FE
    @GetMapping("/hello")
    public Map<String, String> sayHello() {
        return Map.of("message", "Chào Hiếu! Backend Spring Boot đã nhận diện được bạn.");
    }

    // 2. Endpoint test kết nối DB (Dùng để chuẩn bị cho test ACID)
    @GetMapping("/wallet-check")
    public Map<String, Object> checkWallet() {
        try {
            // Lệnh SQL lấy số dư của Hiếu trong DB
            String sql = "SELECT balance FROM wallets WHERE worker_name LIKE '%Hiếu%' LIMIT 1";
            
            Double balance = jdbcTemplate.queryForObject(sql, Double.class);
            
            return Map.of(
                "status", "✅ Thành công",
                "database", "PostgreSQL (Docker)",
                "worker", "Hiếu Nguyễn",
                "balance", balance
            );
        } catch (Exception e) {
            return Map.of(
                "status", "❌ Thất bại",
                "reason", "Chưa có bảng 'wallets' hoặc chưa có dữ liệu mẫu.",
                "error", e.getMessage()
            );
        }
    }
}