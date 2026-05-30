package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "stock_entries")
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class StockEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @Column(name = "total_price")
    private BigDecimal totalPrice;

    @Column(name = "entry_date")
    private LocalDateTime entryDate;

    @OneToMany(mappedBy = "stockEntry", cascade = CascadeType.ALL)
    private List<StockEntryItem> items;

    @PrePersist
    protected void onCreate() {
        this.entryDate = LocalDateTime.now();
    }
}