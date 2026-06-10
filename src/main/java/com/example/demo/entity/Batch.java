package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "batches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Batch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "batch_code", length = 50, nullable = false)
    private String batchCode;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    public void decreaseQuantity(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Số lượng trừ phải lớn hơn 0");
        }
        if (this.quantity < amount) {
            throw new RuntimeException("Lô " + batchCode + " không đủ hàng!");
        }
        this.quantity -= amount;
    }
}
