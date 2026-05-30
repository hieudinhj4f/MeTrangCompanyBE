package com.example.demo.dto.response;

import com.example.demo.entity.Warehouse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseResponse {

    private Integer id;
    private String name;
    private String address;
    private Boolean isActive;

    public static WarehouseResponse from(Warehouse warehouse) {
        return WarehouseResponse.builder()
                .id(warehouse.getId())
                .name(warehouse.getName())
                .address(warehouse.getAddress())
                .isActive(warehouse.getIsActive())
                .build();
    }
}
