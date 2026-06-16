package com.example.demo.service;

import com.example.demo.entity.*;
import com.example.demo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final TransactionHistoryRepository transactionRepository;
    private final CustomerService customerService;
    private final UserRepository userRepository;

    @Transactional
    public Wallet depositMoney(UUID customerOrUserId, BigDecimal amount, UUID performedBy) {
        Customer customer = customerService.resolveOrCreateCustomer(customerOrUserId);

        Wallet wallet = walletRepository.findByCustomerId(customer.getId())
                .orElseGet(() -> walletRepository.save(Wallet.builder()
                        .customer(customer)
                        .balance(BigDecimal.ZERO)
                        .build()));

        wallet.setBalance(wallet.getBalance().add(amount));
        Wallet saved = walletRepository.save(wallet);

        String performedByName = "Hệ thống";
        if (performedBy != null) {
            performedByName = userRepository.findById(performedBy)
                    .map(u -> u.getFullName() != null ? u.getFullName() : u.getUsername())
                    .orElse("Không xác định");
        }

        transactionRepository.save(TransactionHistory.builder()
                .wallet(saved)
                .amount(amount)
                .type("DEPOSIT")
                .description("Nạp tiền vào ví")
                .createdAt(LocalDateTime.now())
                .performedBy(performedBy)
                .performedByName(performedByName)
                .build());

        return saved;
    }
    public Wallet getWalletByCustomerId(UUID customerId) {
        return walletRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ví cho khách hàng: " + customerId));
    }

    public java.util.Optional<Wallet> getWalletByCustomerIdOptional(UUID customerId) {
        return walletRepository.findByCustomerId(customerId);
    }
    public List<TransactionHistory> getTransactionHistory(UUID customerId) {
        // 1. Tìm ví của khách hàng trước
        Wallet wallet = walletRepository.findByCustomerId(customerId).orElse(null);
        if (wallet == null) {
            return java.util.Collections.emptyList();
        }

        // 2. Gọi Repository theo đúng tên hàm bạn đã định nghĩa trong ảnh
        return transactionRepository.findByWalletOrderByCreatedAtDesc(wallet);
    }

    public List<TransactionHistory> getHistoryByPerformedBy(UUID performedBy) {
        return transactionRepository.findByPerformedByOrderByCreatedAtDesc(performedBy);
    }

    @Transactional
    public List<Wallet> depositMoneyBulk(List<UUID> customerOrUserIds, BigDecimal amount, UUID performedBy) {
        return customerOrUserIds.stream()
                .map(id -> depositMoney(id, amount, performedBy))
                .toList();
    }
}