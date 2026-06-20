package com.example.demo.controller;

import com.example.demo.dto.request.UserRequest;
import com.example.demo.dto.response.UserResponse;
import com.example.demo.entity.User;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Optional;
import jakarta.servlet.http.HttpServletRequest;
import com.example.demo.security.JwtAuthFilter;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> responses = userService.getAllUsers().stream()
                .map(user -> UserResponse.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .fullName(user.getFullName())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .role(user.getRole())
                        .isActive(user.getIsActive())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/enterprise/workers")
    public ResponseEntity<?> getEnterpriseWorkers(HttpServletRequest request) {
        String role = (String) request.getAttribute(JwtAuthFilter.ATTR_ROLE);
        if (!"ENTERPRISE".equals(role)) {
            return ResponseEntity.status(403).body(Map.of("reason", "Chỉ doanh nghiệp mới được phép xem danh sách này."));
        }
        UUID authCustomerId = (UUID) request.getAttribute(JwtAuthFilter.ATTR_CUSTOMER_ID);
        if (authCustomerId == null) {
            return ResponseEntity.status(403).body(Map.of("reason", "Tài khoản doanh nghiệp chưa được thiết lập Hồ sơ Khách hàng."));
        }
        
        List<UserResponse> responses = userService.getWorkersByEnterprise(authCustomerId).stream()
                .map(user -> UserResponse.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .fullName(user.getFullName())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .role(user.getRole())
                        .isActive(user.getIsActive())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID id) {
        return userService.getUserById(id)
                .map(user -> ResponseEntity.ok(UserResponse.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .fullName(user.getFullName())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .role(user.getRole())
                        .isActive(user.getIsActive())
                        .build()))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody UserRequest request) {
        User user = User.builder()
                .username(request.getUsername())
                .password(request.getPassword())
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .role(request.getRole())
                .isActive(true)
                .build();

        User savedUser = userService.saveUser(user, request.getEnterpriseId());

        UserResponse response = UserResponse.builder()
                .id(savedUser.getId())
                .username(savedUser.getUsername())
                .fullName(savedUser.getFullName())
                .role(savedUser.getRole())
                .isActive(savedUser.getIsActive())
                .customerId(savedUser.getCustomer() != null ? savedUser.getCustomer().getId() : null)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Cập nhật thông tin nhân viên
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable UUID id, @RequestBody UserRequest userDetails, HttpServletRequest request) {
        Optional<User> userOpt = userService.getUserById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();

            String role = (String) request.getAttribute(JwtAuthFilter.ATTR_ROLE);
            UUID authCustomerId = (UUID) request.getAttribute(JwtAuthFilter.ATTR_CUSTOMER_ID);

            if ("ENTERPRISE".equals(role)) {
                // Kiểm tra xem user này có đúng là worker của enterprise này không
                if (user.getCustomer() == null || !authCustomerId.equals(user.getCustomer().getEnterpriseId())) {
                    return ResponseEntity.status(403).body(Map.of("reason", "Bạn chỉ được phép chỉnh sửa nhân viên của công ty mình!"));
                }
                // CHẶN NÂNG QUYỀN: Bắt buộc ép role về CUSTOMER (tức là Worker trong bối cảnh B2B)
                userDetails.setRole(User.Role.CUSTOMER);
            } else if (!"ADMIN".equals(role)) {
                return ResponseEntity.status(403).body(Map.of("reason", "Bạn không có quyền thực hiện thao tác này!"));
            }

            user.setFullName(userDetails.getFullName());
            user.setRole(userDetails.getRole());
            user.setEmail(userDetails.getEmail());
            user.setPhone(userDetails.getPhone());

            // Cho phép đổi mật khẩu nếu có truyền lên
            if (userDetails.getPassword() != null && !userDetails.getPassword().trim().isEmpty()) {
                user.setPassword(userDetails.getPassword().trim());
            }
            
            User savedUser = userService.saveUser(user, userDetails.getEnterpriseId());
            UserResponse response = UserResponse.builder()
                    .id(savedUser.getId())
                    .username(savedUser.getUsername())
                    .fullName(savedUser.getFullName())
                    .email(savedUser.getEmail())
                    .phone(savedUser.getPhone())
                    .role(savedUser.getRole())
                    .isActive(savedUser.getIsActive())
                    .customerId(savedUser.getCustomer() != null ? savedUser.getCustomer().getId() : null)
                    .build();
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.notFound().build();
    }


}