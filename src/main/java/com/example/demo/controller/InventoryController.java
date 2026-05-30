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
    public ResponseEntity<?> addStock(@RequestBody Map<String, Integer> request) {
        try {
            Integer warehouseId = request.get("warehouseId");
            Long productId = request.get("productId").longValue();
            Integer amount = request.get("amount");

            inventoryService.addStock(warehouseId, productId, amount);

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
    @GetMapping("/expired-alerts")
    public ResponseEntity<?> getExpiredAlerts(@RequestParam(defaultValue = "30") Integer days) {
        LocalDate targetDate = LocalDate.now().plusDays(days);
        List<InventoryItemResponse> expiredProducts = inventoryRepository.findExpiredProducts(targetDate).stream()
                .map(InventoryItemResponse::from)
                .toList();

        return ResponseEntity.ok(Map.of(
                "checkDate", targetDate,
                "totalExpiredCount", expiredProducts.size(),
                "products", expiredProducts));
    }
}