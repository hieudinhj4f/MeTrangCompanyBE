package com.example.demo.entity;

import jakarta.persistence.Embeddable;
import lombok.*;
import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryId implements Serializable {
    private Integer warehouseId; 
    private Long productId;      
}