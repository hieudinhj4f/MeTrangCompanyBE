package com.example.demo.controller;

import com.example.demo.dto.request.ReceiptRequest;
import com.example.demo.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @GetMapping
    public ResponseEntity<?> getAllWarehouses() {
        return ResponseEntity.ok(warehouseService.getAllWarehouses());
    }

    @GetMapping("/{warehouseId}")
    public ResponseEntity<?> getWarehouse(@PathVariable Integer warehouseId) {
        return ResponseEntity.ok(warehouseService.getWarehouseById(warehouseId));
    }

    @GetMapping("/{warehouseId}/inventory")
    public ResponseEntity<?> getWarehouseInventory(@PathVariable Integer warehouseId) {
        return ResponseEntity.ok(warehouseService.getInventoryByWarehouse(warehouseId));
    }

    @GetMapping("/{warehouseId}/transactions")
    public ResponseEntity<?> getWarehouseTransactions(@PathVariable Integer warehouseId) {
        return ResponseEntity.ok(warehouseService.getTransactionsByWarehouse(warehouseId));
    }

    @PostMapping("/import")
    public ResponseEntity<?> importStock(@RequestBody ReceiptRequest request) {
        try {
            warehouseService.createImportStockEntry(request);
            return ResponseEntity.ok("Phiếu nhập kho đã được lưu vào hệ thống StockEntry thành công!");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Lỗi khi ghi nhận dữ liệu nhập kho: " + e.getMessage());
        }
    }
}
