package com.example.demo.controller;

import com.example.demo.dto.request.LoginRequest;
import com.example.demo.dto.request.RegisterRequest;
import com.example.demo.dto.response.LoginResponse;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtService;
import com.example.demo.service.CustomerService;
import com.example.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final CustomerService customerService;
    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        return userRepository.findByUsername(request.getUsername())
                .filter(user -> user.getPassword().equals(request.getPassword()))
                .filter(user -> Boolean.TRUE.equals(user.getIsActive()))
                .<ResponseEntity<?>>map(user -> ResponseEntity.ok(buildLoginResponse(user)))
                .orElseGet(() -> ResponseEntity.status(401)
                        .body(Map.of("reason", "Sai tài khoản hoặc mật khẩu")));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("reason", "Username không được để trống"));
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("reason", "Mật khẩu không được để trống"));
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest().body(Map.of("reason", "Username đã tồn tại!"));
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(request.getPassword())
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .role(User.Role.CUSTOMER)
                .isActive(true)
                .build();

        User savedUser = userService.saveUser(user);
        return ResponseEntity.ok(buildLoginResponse(savedUser));
    }

    private LoginResponse buildLoginResponse(User user) {
        UUID customerId = null;
        if (user.getRole() == User.Role.CUSTOMER || user.getRole() == User.Role.ENTERPRISE) {
            customerId = customerService.ensureCustomerForUser(user).getId();
        } else if (user.getCustomer() != null) {
            customerId = user.getCustomer().getId();
        }

        String token = jwtService.generateToken(
                user.getId(),
                customerId,
                user.getRole().name(),
                user.getUsername()
        );

        return new LoginResponse(
                token,
                user.getId(),
                customerId,
                user.getUsername(),
                user.getRole().name()
        );
    }

    @GetMapping("/seed-workers")
    public ResponseEntity<?> seedWorkers(@RequestParam(defaultValue = "100") int count) {
        int successCount = 0;
        for (int i = 1; i <= count; i++) {
            String username = "congnhan_test_" + System.currentTimeMillis() + "_" + i;
            User user = User.builder()
                    .username(username)
                    .password("123456")
                    .fullName("Công Nhân " + i)
                    .email(username + "@metrang.com.vn")
                    .phone("09" + String.format("%08d", (int)(Math.random() * 100000000)))
                    .role(User.Role.CUSTOMER)
                    .isActive(true)
                    .build();
            try {
                User savedUser = userService.saveUser(user);
                customerService.ensureCustomerForUser(savedUser); // Tự động tạo Customer và Wallet
                successCount++;
            } catch (Exception e) {
                // Ignore duplicates or errors
            }
        }
        return ResponseEntity.ok(Map.of("message", "Đã tạo thành công " + successCount + " công nhân mẫu cùng với ví điện tử!"));
    }
}
