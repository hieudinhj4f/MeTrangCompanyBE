package com.example.demo.service;

import com.example.demo.entity.Customer;
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

    Customer customer = Customer.builder()
            .id(user.getId()) 
            .fullName(user.getFullName() != null ? user.getFullName() : user.getUsername())
            .email(user.getEmail())
            .phoneNumber(user.getPhone())
            .totalSpent(BigDecimal.ZERO)
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
                        .orElseGet(() -> {
                            Customer newCustomer = Customer.builder()
                                    .id(id)
                                    .fullName("Khách hàng mới")
                                    .totalSpent(BigDecimal.ZERO)
                                    .build();
                            Customer saved = customerRepository.saveAndFlush(newCustomer);
                            ensureWalletExists(saved);
                            return saved;
                        }));
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
}
