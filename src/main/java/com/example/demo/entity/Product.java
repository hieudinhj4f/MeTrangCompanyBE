package com.example.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder 
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String sku; 

    @Column(nullable = false)
    private String name; 

    @Column(name = "base_price", nullable = false)
    private BigDecimal basePrice; 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    @JsonIgnoreProperties({"products", "handler", "hibernateLazyInitializer"})
    private Category category; 

    @Column(columnDefinition = "TEXT") 
    private String description; 

    @Column(name = "is_best_seller")
    private Boolean isBestSeller = false; 

    @Column(name = "is_ingredient")
    private Boolean isIngredient = false; 

    @Column(name = "is_active")
    @JsonProperty("active")
    private Boolean active = true;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @JsonProperty("unit")
    public String getUnit() {
        return Boolean.TRUE.equals(isIngredient) ? "Kg" : "Cái";
    }
}