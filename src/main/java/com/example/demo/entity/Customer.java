package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    public enum CustomerType{
        RETAIL,
        WORKER,
        ENTERPRISE
    }

    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
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

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "tax_code", unique = true)
    private String taxCode; 

    @Column(name = "billing_address")
    private String billingAddress;

    @Column(name = "customer_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private CustomerType customerType;

    public BigDecimal getTotalSpent() {
        return totalSpent == null ? BigDecimal.ZERO : totalSpent;
    }

    @OneToOne(mappedBy = "customer", cascade = CascadeType.ALL)
    private Wallet wallet;
}