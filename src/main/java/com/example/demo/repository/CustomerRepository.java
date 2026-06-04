package com.example.demo.repository;

import com.example.demo.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    
    // Tìm kiếm khách hàng theo Email (thường dùng cho đăng nhập)
    Optional<Customer> findByEmail(String email);

    Optional<Customer> findByPhoneNumber(String phoneNumber);

    Optional<Customer> findByFullName(String fullName);

    Optional<Customer> findByTaxCode(String taxCode);

    // 2. Lấy danh sách khách hàng theo phân loại (RETAIL, WORKER, ENTERPRISE)
    List<Customer> findByCustomerType(Customer.CustomerType customerType);

    
}