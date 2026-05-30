package com.example.demo.controller;

import com.example.demo.dto.request.LoginRequest;
import com.example.demo.dto.request.RegisterRequest;
import com.example.demo.dto.responde.LoginResponse;
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
        if (user.getRole() == User.Role.CUSTOMER) {
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
}
