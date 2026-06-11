package com.example.demo.dto.response;

import com.example.demo.entity.Customer;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class CustomerProfileResponse {
    private UUID id;
    private String fullName;
    private String phoneNumber;
    private String email;
    private BigDecimal totalSpent;

    public static CustomerProfileResponse from(Customer customer) {
        return CustomerProfileResponse.builder()
                .id(customer.getId())
                .fullName(customer.getFullName())
                .phoneNumber(customer.getPhoneNumber())
                .email(customer.getEmail())
                .totalSpent(customer.getTotalSpent())
                .build();
    }
}
