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
    public ResponseEntity<User> updateUser(@PathVariable UUID id, @RequestBody UserRequest userDetails) {
        Optional<User> userOpt = userService.getUserById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setFullName(userDetails.getFullName());
            user.setRole(userDetails.getRole());
            user.setEmail(userDetails.getEmail());
            user.setPhone(userDetails.getPhone());
            return ResponseEntity.ok(userService.saveUser(user, userDetails.getEnterpriseId()));
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Vô hiệu hóa tài khoản (Soft Delete)
     */
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateUser(@PathVariable UUID id) {
        userService.deactivateUser(id);
        return ResponseEntity.ok().build();
    }
}