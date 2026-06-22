package com.example.demo.repository;

import com.example.demo.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByEmail(String email);

    Optional<Customer> findByPhoneNumber(String phoneNumber);

    Optional<Customer> findByFullName(String fullName);

    Optional<Customer> findByTaxCode(String taxCode);

    List<Customer> findByCustomerType(Customer.CustomerType customerType);

    List<Customer> findByEnterpriseId(UUID enterpriseId);
}