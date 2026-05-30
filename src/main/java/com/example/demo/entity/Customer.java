package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "customers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    private UUID id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "phone_number", unique = true)
    private String phoneNumber;

    @Column(unique = true)
    private String email;

    @Column(name = "total_spent", nullable = false, columnDefinition = "numeric(38,2) default 0")
    private BigDecimal totalSpent;

    @ManyToOne
    @JoinColumn(name = "rank_id") 
    private Rank rank;

    public BigDecimal getTotalSpent() {
        return totalSpent == null ? BigDecimal.ZERO : totalSpent;
    }
}