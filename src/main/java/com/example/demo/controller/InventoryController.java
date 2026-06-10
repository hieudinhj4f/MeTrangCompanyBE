package com.example.demo.controller;

import com.example.demo.dto.response.InventoryItemResponse;
import com.example.demo.repository.InventoryRepository;
import com.example.demo.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;
    private final InventoryRepository inventoryRepository;

    @PostMapping("/add-stock")
    public ResponseEntity<?> addStock(@RequestBody Map<String, Object> request) {
        try {
            Integer warehouseId = request.get("warehouseId") != null ? Integer.valueOf(request.get("warehouseId").toString()) : null;
            Long productId = request.get("productId") != null ? Long.valueOf(request.get("productId").toString()) : null;
            Integer amount = request.get("amount") != null ? Integer.valueOf(request.get("amount").toString()) : null;

            String batchCode = request.get("batchCode") != null ? String.valueOf(request.get("batchCode")) : "TEMP-BATCH";
            LocalDate expiryDate = request.get("expiryDate") != null ? LocalDate.parse(String.valueOf(request.get("expiryDate"))) : LocalDate.now().plusMonths(6);

            inventoryService.addStockWithBatch(warehouseId, productId, amount, batchCode, expiryDate);

            return ResponseEntity.ok(Map.of(
                    "status", "Thành công",
                    "message", "Đã cập nhật số lượng kho mới"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "Thất bại",
                    "reason", e.getMessage()));
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllInventory() {
        List<InventoryItemResponse> list = inventoryService.getAllInventoryItems();

        if (list.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "message", "Kho hiện đang trống, vui lòng nhập hàng!",
                    "data", list));
        }

        return ResponseEntity.ok(list);
    }

    /**
     * 3. Cảnh báo tồn kho thấp (Dưới mức threshold, mặc định là 10)
     */
    @GetMapping("/low-stock-alerts/{warehouseId}")
    public ResponseEntity<?> getLowStockAlerts(
            @PathVariable Integer warehouseId,
            @RequestParam(defaultValue = "10") Integer threshold) {

        List<InventoryItemResponse> lowStockList = inventoryRepository.findLowStock(warehouseId, threshold).stream()
                .map(InventoryItemResponse::from)
                .toList();
        return ResponseEntity.ok(lowStockList);
    }

    /**
     * 4. Cảnh báo thuốc sắp hết hạn (Trong vòng X ngày tới)
     * API: /expired-alerts?days=30
     */
    private final com.example.demo.repository.BatchRepository batchRepository;

    @GetMapping("/expired-alerts")
    public ResponseEntity<?> getExpiredAlerts(@RequestParam(defaultValue = "30") Integer days) {
        LocalDate targetDate = LocalDate.now().plusDays(days);
        List<com.example.demo.entity.Batch> expiredBatches = batchRepository.findExpiredBatches(targetDate);

        var responseList = expiredBatches.stream().map(b -> Map.of(
                "batchCode", b.getBatchCode(),
                "productName", b.getProduct().getName(),
                "quantity", b.getQuantity(),
                "expiryDate", b.getExpiryDate()
        )).toList();

        return ResponseEntity.ok(Map.of(
                "checkDate", targetDate,
                "totalExpiredCount", responseList.size(),
                "batches", responseList));
    }
}