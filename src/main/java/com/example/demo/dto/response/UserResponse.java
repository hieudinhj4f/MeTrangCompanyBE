package com.example.demo.dto.response;

import com.example.demo.entity.User.Role;
import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class UserResponse {
    private UUID id;
    private String username;
    private String fullName;
    private String email;
    private String phone;
    private Role role;
    private Boolean isActive;
    private UUID customerId;
    private UUID enterpriseId;
}   