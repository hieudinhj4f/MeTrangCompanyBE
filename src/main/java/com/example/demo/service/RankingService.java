package com.example.demo.service;

import com.example.demo.entity.Customer;
import com.example.demo.entity.Rank;
import com.example.demo.repository.CustomerRepository;
import com.example.demo.repository.RankRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RankingService {

    private final CustomerRepository customerRepository;
    private final RankRepository rankRepository;

    /**
     * Kiểm tra và tự động nâng hạng dựa trên tổng chi tiêu
     */
    @Transactional
    public void checkAndUpgradeRank(Customer customer) {
        // Giả sử: 1000đ = 1 điểm. Hiếu có thể đổi tùy ý.
        int currentPoints = customer.getTotalSpent().divide(new BigDecimal("1000")).intValue();

        List<Rank> allRanks = rankRepository.findAll();

        Rank eligibleRank = allRanks.stream()
                .filter(r -> r.getMinPoint() != null && currentPoints >= r.getMinPoint())
                .max(Comparator.comparingInt(Rank::getMinPoint))
                .orElse(customer.getRank());

        if (eligibleRank != null
                && (customer.getRank() == null || !eligibleRank.getId().equals(customer.getRank().getId()))) {
            customer.setRank(eligibleRank);
            customerRepository.save(customer);
        }
    }
    public List<Rank> getAllRanks() {
        return rankRepository.findAll();
    }

    // Lấy thông tin một hạng theo ID
    public Rank getRankById(Integer id) {
        return rankRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Rank với ID: " + id));
    }

    // Tạo mới một hạng thành viên
    public Rank createRank(Rank rank) {
        // Có thể thêm logic kiểm tra trùng tên hạng ở đây nếu muốn
        return rankRepository.save(rank);
    }

    // Cập nhật thông tin hạng
    public Rank updateRank(Integer id, Rank rankDetails) {
        Rank existingRank = getRankById(id);
        
        existingRank.setRankName(rankDetails.getRankName());
        existingRank.setMinPoint(rankDetails.getMinPoint());
        existingRank.setDiscountRate(rankDetails.getDiscountRate());
        existingRank.setDescription(rankDetails.getDescription());

        return rankRepository.save(existingRank);
    }

    // Xóa hạng
    public void deleteRank(Integer id) {
        Rank existingRank = getRankById(id);
        rankRepository.delete(existingRank);
    }
    
}