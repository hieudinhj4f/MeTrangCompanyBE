package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "warehouse_receipts")
@Data
public class WarehouseReceipt {

    public enum status {
        PENDING, COMPLETED, CANCELLED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String receiptCode;

    private String type;

    private String supplierId;

    private String delivererName;

    private String staffName;

    private String status;

    private String totalAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)

    private Warehouse warehouse;

    private LocalDateTime createdAt;

    @Column(name = "approved_by", nullable = true)
    private UUID ApprovedBy;

    @Column(name = "approved_at", nullable = true)
    private LocalDateTime approvedAt;

    @OneToMany(mappedBy = "receipt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WarehouseReceiptDetails> items = new ArrayList<>();
}
