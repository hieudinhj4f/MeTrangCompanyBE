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
        Customer targetCustomer = customerService.resolveOrCreateCustomer(customerOrUserId);

        String performedByName = "Hệ thống";
        if (performedBy != null) {
            User performerUser = userRepository.findById(performedBy).orElse(null);
            if (performerUser != null) {
                performedByName = performerUser.getFullName() != null ? performerUser.getFullName() : performerUser.getUsername();
                
                // Nếu là doanh nghiệp, thực hiện trừ tiền và kiểm tra hạn mức
                if (performerUser.getRole() == User.Role.ENTERPRISE) {
                    Customer enterpriseCustomer = performerUser.getCustomer();
                    if (enterpriseCustomer == null) {
                        throw new RuntimeException("Tài khoản doanh nghiệp chưa được thiết lập hồ sơ!");
                    }
                    Wallet enterpriseWallet = getWalletByCustomerId(enterpriseCustomer.getId());
                    BigDecimal newBalance = enterpriseWallet.getBalance().subtract(amount);
                    
                    BigDecimal creditLimit = enterpriseCustomer.getCreditLimit() != null ? enterpriseCustomer.getCreditLimit() : BigDecimal.ZERO;
                    if (newBalance.compareTo(creditLimit.negate()) < 0) {
                        throw new RuntimeException("Giao dịch vượt quá hạn mức tín dụng cho phép! Số dư khả dụng còn lại: " + enterpriseWallet.getBalance().add(creditLimit).toString());
                    }
                    
                    enterpriseWallet.setBalance(newBalance);
                    walletRepository.save(enterpriseWallet);
                    
                    transactionRepository.save(TransactionHistory.builder()
                            .wallet(enterpriseWallet)
                            .amount(amount.negate())
                            .type("WITHDRAW")
                            .description("Nạp tiền cho công nhân " + (targetCustomer.getFullName() != null ? targetCustomer.getFullName() : ""))
                            .createdAt(LocalDateTime.now())
                            .performedBy(performedBy)
                            .performedByName(performedByName)
                            .build());
                }
            }
        }

        Wallet targetWallet = walletRepository.findByCustomerId(targetCustomer.getId())
                .orElseGet(() -> walletRepository.save(Wallet.builder()
                        .customer(targetCustomer)
                        .balance(BigDecimal.ZERO)
                        .build()));

        targetWallet.setBalance(targetWallet.getBalance().add(amount));
        Wallet savedTargetWallet = walletRepository.save(targetWallet);

        transactionRepository.save(TransactionHistory.builder()
                .wallet(savedTargetWallet)
                .amount(amount)
                .type("DEPOSIT")
                .description("Nạp tiền vào ví")
                .createdAt(LocalDateTime.now())
                .performedBy(performedBy)
                .performedByName(performedByName)
                .build());

        return savedTargetWallet;
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

    @Transactional
    public String fixHistoricalEnterpriseBalances() {
        // Lấy danh sách tất cả các user có role là ENTERPRISE
        List<User> enterprises = userRepository.findAll().stream()
                .filter(u -> u.getRole() == User.Role.ENTERPRISE)
                .toList();

        int fixedCount = 0;
        BigDecimal totalDeducted = BigDecimal.ZERO;

        for (User enterprise : enterprises) {
            Customer customer = enterprise.getCustomer();
            if (customer == null) continue;

            Wallet wallet = walletRepository.findByCustomerId(customer.getId()).orElse(null);
            if (wallet == null) continue;

            // Tính tổng số tiền doanh nghiệp này đã nạp cho người khác trong quá khứ (type = DEPOSIT, performedBy = enterprise.getId)
            // Lưu ý: Các giao dịch mới (sau bản fix này) sẽ có giao dịch WITHDRAW riêng của doanh nghiệp. 
            // Ta chỉ tính tổng các giao dịch DEPOSIT mà doanh nghiệp đã thực hiện.
            List<TransactionHistory> deposits = transactionRepository.findByPerformedByOrderByCreatedAtDesc(enterprise.getId()).stream()
                    .filter(t -> "DEPOSIT".equals(t.getType()))
                    .toList();

            BigDecimal historicalTotalDeposited = deposits.stream()
                    .map(TransactionHistory::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Tạm thời trừ thẳng vào ví doanh nghiệp số tiền lịch sử này
            if (historicalTotalDeposited.compareTo(BigDecimal.ZERO) > 0) {
                // Kiểm tra xem đã có giao dịch sửa lỗi chưa để tránh trừ 2 lần
                boolean alreadyFixed = transactionRepository.findByPerformedByOrderByCreatedAtDesc(enterprise.getId()).stream()
                        .anyMatch(t -> "SYSTEM_FIX".equals(t.getType()));

                if (!alreadyFixed) {
                    wallet.setBalance(wallet.getBalance().subtract(historicalTotalDeposited));
                    walletRepository.save(wallet);

                    transactionRepository.save(TransactionHistory.builder()
                            .wallet(wallet)
                            .amount(historicalTotalDeposited.negate())
                            .type("SYSTEM_FIX")
                            .description("Truy thu lịch sử nạp tiền chưa trừ ví")
                            .createdAt(LocalDateTime.now())
                            .performedBy(enterprise.getId())
                            .performedByName("Hệ thống")
                            .build());

                    fixedCount++;
                    totalDeducted = totalDeducted.add(historicalTotalDeposited);
                }
            }
        }
        return "Đã xử lý truy thu cho " + fixedCount + " doanh nghiệp. Tổng tiền truy thu: " + totalDeducted + " VNĐ.";
    }
}