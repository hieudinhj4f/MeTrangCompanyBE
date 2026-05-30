package com.example.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "inventories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventories {

    @EmbeddedId
    private InventoryId id;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("warehouseId")
    @JoinColumn(name = "warehouse_id")
    @JsonIgnore
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("productId")
    @JoinColumn(name = "product_id")
    @JsonIgnoreProperties({"inventories", "handler", "hibernateLazyInitializer"})
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Version
    @Column(nullable = false)
    private Integer version;

    @PrePersist
    @PreUpdate
    private void ensureNonNullFields() {
        if (this.quantity == null) {
            this.quantity = 0;
        }
        if (this.version == null) {
            this.version = 0;
        }
    }

    public void decreaseStock(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Số lượng trừ phải lớn hơn 0");
        }
        if (this.quantity == null) {
            this.quantity = 0;
        }
        if (this.quantity < amount) {
            throw new RuntimeException(
                    "Kho " + (warehouse != null ? warehouse.getName() : "") + " không đủ hàng!");
        }
        this.quantity -= amount;
    }

    public void increaseStock(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Số lượng nhập phải lớn hơn 0");
        }
        if (this.quantity == null) {
            this.quantity = 0;
        }
        this.quantity += amount;
    }
}
