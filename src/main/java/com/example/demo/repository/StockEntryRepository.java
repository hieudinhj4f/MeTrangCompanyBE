package com.example.demo.repository;

import com.example.demo.entity.StockEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

@Repository
public interface StockEntryRepository extends JpaRepository<StockEntry, UUID> {

    List<StockEntry> findBySupplierId(Integer supplierId);

    List<StockEntry> findByWarehouse_Id(Integer warehouseId);

    @Query("SELECT se FROM StockEntry se JOIN FETCH se.warehouse WHERE se.id = :id")
    Optional<StockEntry> findByIdWithWarehouse(@Param("id") Long id);

    @Query("SELECT DISTINCT se FROM StockEntry se LEFT JOIN FETCH se.items i LEFT JOIN FETCH i.product LEFT JOIN FETCH se.supplier WHERE se.warehouse.id = :warehouseId")
    List<StockEntry> findByWarehouseIdWithItems(@Param("warehouseId") Integer warehouseId);
}
