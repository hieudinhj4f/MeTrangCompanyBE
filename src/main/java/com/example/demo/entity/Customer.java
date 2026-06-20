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

    public enum CustomerType {
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

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "tax_code", unique = true)
    private String taxCode;

    @Column(name = "billing_address")
    private String billingAddress;

    @Column(name = "customer_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private CustomerType customerType;

    @Column(name = "credit_limit", columnDefinition = "numeric(38,2)")
    @Builder.Default
    private BigDecimal creditLimit = BigDecimal.ZERO;

    @Column(name = "b2b_discount_rate", columnDefinition = "numeric(5,2)")
    @Builder.Default
    private BigDecimal b2bDiscountRate = BigDecimal.ZERO;

    // UUID of the ENTERPRISE customer if this customer is a WORKER
    @Column(name = "enterprise_id")
    private UUID enterpriseId;

    @OneToOne(mappedBy = "customer", cascade = CascadeType.ALL)
    private Wallet wallet;

    @OneToOne(mappedBy = "customer", fetch = FetchType.EAGER)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"customer", "password"})
    private User user;
}