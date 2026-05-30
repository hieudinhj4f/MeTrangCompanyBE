package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount; 
 
    @Column(nullable = false)
    private String type; 

    @Column(columnDefinition = "TEXT")
    private String description; 

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt; 

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now(); 
    }
}