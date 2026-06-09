package com.example.demo.repository;

import com.example.demo.entity.TransactionHistory;
import com.example.demo.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransactionHistoryRepository extends JpaRepository<TransactionHistory, Long> {
    // Tìm lịch sử theo đối tượng Wallet và sắp xếp thời gian giảm dần
    List<TransactionHistory> findByWalletOrderByCreatedAtDesc(Wallet wallet);

    // Tìm lịch sử nạp tiền do doanh nghiệp/admin thực hiện
    List<TransactionHistory> findByPerformedByOrderByCreatedAtDesc(java.util.UUID performedBy);
}