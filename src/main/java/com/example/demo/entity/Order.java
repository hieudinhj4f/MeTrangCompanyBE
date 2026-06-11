package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter 
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    public enum OrderStatus {
        PENDING,
        PAID,
        PROCESSING,
        SHIPPING,
        DELIVERED,
        COMPLETED,
        CANCELLED
    }

    public enum PaymentMethod {
        CASH, CARD, VNPAY, WALLET, DEBT 
    }

    public enum OrderType {
        IN_STORE, DELIVERY, PRE_ORDER
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Builder.Default 
    @Column(name = "is_priority")
    private Boolean isPriority = false;

    @Column(name = "order_date")
    private LocalDateTime orderDate;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type")
    @Builder.Default
    private OrderType orderType = OrderType.IN_STORE;

    @Column(name = "delivery_address")
    private String deliveryAddress;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default 
    private List<OrderItem> items = new ArrayList<>();

    public void addOrderItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    @PrePersist
    protected void onCreate() {
        this.orderDate = LocalDateTime.now();
    }
}