package com.example.demo.repository;

import com.example.demo.entity.Inventories;
import com.example.demo.entity.InventoryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface InventoryRepository extends JpaRepository<Inventories, InventoryId> {

    @Query("SELECT i FROM Inventories i JOIN FETCH i.product WHERE i.warehouse.id = :warehouseId AND i.quantity < :threshold")
    List<Inventories> findLowStock(Integer warehouseId, Integer threshold);

    @Query("SELECT i FROM Inventories i JOIN FETCH i.product WHERE i.expiryDate <= :targetDate")
    List<Inventories> findExpiredProducts(LocalDate targetDate);

    @Query("SELECT i FROM Inventories i JOIN FETCH i.product JOIN FETCH i.warehouse WHERE i.warehouse.id = :warehouseId")
    List<Inventories> findByWarehouseId(Integer warehouseId);
}
