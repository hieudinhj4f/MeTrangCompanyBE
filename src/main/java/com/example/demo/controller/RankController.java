package com.example.demo.controller;

import com.example.demo.entity.Rank;
import com.example.demo.service.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ranks")
@RequiredArgsConstructor
public class RankController {

    private final RankingService rankingService;

    // Lấy toàn bộ danh sách Hạng (Ví dụ: Đồng, Bạc, Vàng)
    @GetMapping
    public ResponseEntity<List<Rank>> getAllRanks() {
        return ResponseEntity.ok(rankingService.getAllRanks());
    }

    // Lấy chi tiết 1 Hạng theo ID
    @GetMapping("/{id}")
    public ResponseEntity<Rank> getRankById(@PathVariable Integer id) {
        return ResponseEntity.ok(rankingService.getRankById(id));
    }

    // Tạo Hạng mới
    @PostMapping
    public ResponseEntity<Rank> createRank(@RequestBody Rank rank) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rankingService.createRank(rank));
    }

    // Sửa thông tin Hạng
    @PutMapping("/{id}")
    public ResponseEntity<Rank> updateRank(@PathVariable Integer id, @RequestBody Rank rank) {
        return ResponseEntity.ok(rankingService.updateRank(id, rank));
    }

    // Xóa Hạng
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRank(@PathVariable Integer id) {
        rankingService.deleteRank(id);
        return ResponseEntity.noContent().build(); // Trả về 204 No Content khi xóa thành công
    }
}