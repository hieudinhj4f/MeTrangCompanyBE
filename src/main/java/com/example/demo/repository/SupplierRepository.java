package com.example.demo.repository;

import com.example.demo.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Integer> {
    // Hiếu có thể thêm hàm tìm kiếm theo tên nếu cần
    boolean existsByName(String name);
    java.util.Optional<Supplier> findByTaxCode(String taxCode);
}