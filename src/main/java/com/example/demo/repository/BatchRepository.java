package com.example.demo.repository;

import com.example.demo.entity.Batch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BatchRepository extends JpaRepository<Batch, Long> {

    @Query("SELECT b FROM Batch b WHERE b.warehouse.id = :warehouseId AND b.product.id = :productId AND b.quantity > 0 ORDER BY b.expiryDate ASC")
    List<Batch> findAvailableBatchesOrderByExpiryAsc(@Param("warehouseId") Integer warehouseId, @Param("productId") Long productId);

    @Query("SELECT b FROM Batch b JOIN FETCH b.product WHERE b.expiryDate <= :targetDate")
    List<Batch> findExpiredBatches(@Param("targetDate") java.time.LocalDate targetDate);

}
