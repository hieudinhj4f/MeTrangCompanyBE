package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "ranks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) 
    private Integer id;

    @Column(name = "rank_name", nullable = false, unique = true)
    private String rankName;

    @Column(name = "min_point") 
    private Integer minPoint;

    @Column(name = "discount_rate")
    private BigDecimal discountRate;

    @Column(columnDefinition = "TEXT")
    private String description;
}