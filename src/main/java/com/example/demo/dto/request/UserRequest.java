package com.example.demo.dto.request;

import com.example.demo.entity.User.Role;
import lombok.Data;

@Data
public class UserRequest {
    private String username;
    private String password;
    private String fullName;
    private String email;
    private String phone;
    private Role role;
    private Boolean isActive;
}