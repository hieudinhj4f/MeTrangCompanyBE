package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "invoices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", unique = true, nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "customer", "warehouse", "items"})
    private Order order;

    @Column(name = "is_issued")
    @Builder.Default
    private Boolean isIssued = false;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "tax_code", nullable = false)
    private String taxCode;

    @Column(name = "billing_address", nullable = false)
    private String billingAddress;

    @Column(name = "e_invoice_number")
    private String eInvoiceNumber;

    @Column(name = "tax_rate", columnDefinition = "numeric(5,2)")
    @Builder.Default
    private BigDecimal taxRate = new BigDecimal("8.00");

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
