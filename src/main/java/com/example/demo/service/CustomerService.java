package com.example.demo.service;

import com.example.demo.entity.Customer;
import com.example.demo.entity.Customer.CustomerType;
import com.example.demo.entity.User;
import com.example.demo.entity.Wallet;
import com.example.demo.repository.CustomerRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.Optional;
import java.util. List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;

    @Transactional
    public UUID ensureCustomerForUserId(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản: " + userId));
        return ensureCustomerForUser(user).getId();
    }

    @Transactional
public Customer ensureCustomerForUser(User user) {
    if (user.getCustomer() != null) {
        ensureWalletExists(user.getCustomer());
        return user.getCustomer();
    }

    CustomerType type = CustomerType.RETAIL;
    BigDecimal defaultCreditLimit = null;
    if (user.getRole() == User.Role.ENTERPRISE) {
        type = CustomerType.ENTERPRISE;
        defaultCreditLimit = new BigDecimal("50000000"); // 50M default limit
    }

    Customer customer = Customer.builder()
            .fullName(user.getFullName() != null ? user.getFullName() : user.getUsername())
            .email(user.getEmail())
            .phoneNumber(user.getPhone())
            .customerType(type)
            .creditLimit(defaultCreditLimit)
            .build();
            
    customer = customerRepository.saveAndFlush(customer);

    ensureWalletExists(customer);

    user.setCustomer(customer);
    userRepository.save(user);
    return customer;
}


    @Transactional
    public Customer resolveOrCreateCustomer(UUID id) {
        return customerRepository.findById(id)
                .orElseGet(() -> userRepository.findById(id)
                        .map(this::ensureCustomerForUser)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản hoặc khách hàng với ID cung cấp")));
    }

    private void ensureWalletExists(Customer customer) {
        // Với @MapsId, ID của Wallet trùng với ID của Customer
        walletRepository.findById(customer.getId())
                .orElseGet(() -> walletRepository.saveAndFlush(Wallet.builder()
                        .customer(customer) // Hibernate tự lấy ID từ customer gán vào Wallet
                        .balance(BigDecimal.ZERO)
                        .build()));
    }

    public Optional<Customer> getCustomerById(UUID id) {
        // Tìm kiếm khách hàng theo ID vạn năng đã đồng bộ
        return customerRepository.findById(id);
    }
    
    // =========================================================================
    // CÁC HÀM XỬ LÝ RIÊNG CHO ĐỐI TÁC B2B (ENTERPRISE)
    // =========================================================================

    @Transactional(readOnly = true)
    public List<Customer> getWorkersByEnterprise(UUID enterpriseId) {
        return customerRepository.findByEnterpriseId(enterpriseId);
    }

    @Transactional(readOnly = true)
    public Customer searchCustomerForPOS(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new IllegalArgumentException("Từ khóa tìm kiếm không được để trống!");
        }
        
        String cleanKeyword = keyword.trim();
        
        // 1. Tìm theo Số điện thoại
        Optional<Customer> byPhone = customerRepository.findByPhoneNumber(cleanKeyword);
        if (byPhone.isPresent()) {
            return byPhone.get();
        }
        
        // 2. Nếu không thấy, tìm tiếp theo Mã số thuế (B2B)
        Optional<Customer> byTaxCode = customerRepository.findByTaxCode(cleanKeyword);
        if (byTaxCode.isPresent()) {
            return byTaxCode.get();
        }
        
        throw new IllegalArgumentException("Không tìm thấy hồ sơ khách hàng nào khớp với: " + cleanKeyword);
    }

    @Transactional(readOnly = true)
    public List<Customer> getAllB2BCustomers() {
        return customerRepository.findByCustomerType(Customer.CustomerType.ENTERPRISE);
    }

    @Transactional
    public Customer updateB2BCustomer(UUID id, Customer updateData) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));
        
        if (customer.getCustomerType() != Customer.CustomerType.ENTERPRISE) {
            throw new RuntimeException("Khách hàng này không phải là Doanh nghiệp (ENTERPRISE)");
        }
        
        if (updateData.getCompanyName() != null) customer.setCompanyName(updateData.getCompanyName());
        if (updateData.getTaxCode() != null) customer.setTaxCode(updateData.getTaxCode());
        if (updateData.getCreditLimit() != null) customer.setCreditLimit(updateData.getCreditLimit());
        if (updateData.getB2bDiscountRate() != null) customer.setB2bDiscountRate(updateData.getB2bDiscountRate());
        
        return customerRepository.save(customer);
    }
}
