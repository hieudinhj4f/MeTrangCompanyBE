package com.example.demo.dto.responde;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private UUID id;
    private UUID customerId;
    private String username;
    private String role;
}