package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

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
    @JsonIgnore
    private Wallet wallet;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount; 
 
    @Column(nullable = false)
    private String type; 

    @Column(columnDefinition = "TEXT")
    private String description; 

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt; 

    @Column(name = "performed_by")
    private UUID performedBy;

    @Column(name = "performed_by_name")
    private String performedByName;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now(); 
    }
}