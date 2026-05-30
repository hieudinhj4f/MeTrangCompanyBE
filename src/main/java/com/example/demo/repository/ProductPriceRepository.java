package com.example.demo.repository;

import com.example.demo.entity.ProductPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ProductPriceRepository extends JpaRepository<ProductPrice, Long> {

    // Tìm giá của sản phẩm có hiệu lực tại thời điểm 'now'
    @Query("SELECT p FROM ProductPrice p WHERE p.product.id = :productId " +
           "AND p.startDate <= :now " +
           "AND (p.endDate IS NULL OR p.endDate >= :now) " +
           "ORDER BY p.startDate DESC")
    Optional<ProductPrice> findCurrentPrice(@Param("productId") Long productId, @Param("now") LocalDateTime now);
}