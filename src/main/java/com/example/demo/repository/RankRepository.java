package com.example.demo.repository;

import com.example.demo.entity.Rank;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RankRepository extends JpaRepository<Rank, Integer> {
    // Chỉ lấy danh sách, không dùng Query phức tạp nữa
    List<Rank> findAllByOrderByMinPointDesc();

    Optional<Rank> findByRankName(String rankName);
}
