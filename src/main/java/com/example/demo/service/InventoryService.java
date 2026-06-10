package com.example.demo.service;

import com.example.demo.dto.response.InventoryItemResponse;
import com.example.demo.entity.Inventories;
import com.example.demo.entity.InventoryId;
import com.example.demo.entity.Product;
import com.example.demo.entity.Warehouse;
import com.example.demo.repository.InventoryRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final com.example.demo.repository.BatchRepository batchRepository;

    @Transactional
    public Inventories adjustStock(InventoryId id, int adjustment) {
        Inventories inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không có trong kho này!"));

        int newQuantity = inventory.getQuantity() + adjustment;
        
        if (newQuantity < 0) {
            throw new RuntimeException("Số lượng xuất vượt quá tồn kho hiện tại!");
        }

        inventory.setQuantity(newQuantity);
        return inventoryRepository.save(inventory);
    }


    @Transactional
    public void addStockWithBatch(Integer warehouseId, Long productId, Integer amount, String batchCode, java.time.LocalDate expiryDate) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Số lượng nhập phải lớn hơn 0");
        }

        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy kho!"));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm!"));

        // 1. Lưu lô hàng
        com.example.demo.entity.Batch newBatch = com.example.demo.entity.Batch.builder()
                .warehouse(warehouse)
                .product(product)
                .batchCode(batchCode)
                .quantity(amount)
                .expiryDate(expiryDate)
                .build();
        batchRepository.save(newBatch);

        // 2. Cập nhật Inventories
        InventoryId invId = new InventoryId(warehouseId, productId);
        Inventories inventory = inventoryRepository.findById(invId).orElse(null);
        if (inventory != null) {
            inventory.increaseStock(amount);
            inventoryRepository.save(inventory);
        } else {
            Inventories newInventory = new Inventories();
            newInventory.setId(invId);
            newInventory.setWarehouse(warehouse);
            newInventory.setProduct(product);
            newInventory.setQuantity(amount);
            inventoryRepository.save(newInventory);
        }
    }

    @Transactional
    public void exportIngredient(Integer warehouseId, Long productId, int totalQuantityNeeded) {
        if (totalQuantityNeeded <= 0) return;

        InventoryId invId = new InventoryId(warehouseId, productId);
        Inventories inventory = inventoryRepository.findById(invId)
                .orElseThrow(() -> new RuntimeException("Nguyên liệu không có trong kho!"));

        if (inventory.getQuantity() < totalQuantityNeeded) {
            throw new RuntimeException("Kho không đủ số lượng nguyên liệu (Cần: " + totalQuantityNeeded + ", Còn: " + inventory.getQuantity() + ")");
        }

        // FEFO: Trừ theo lô hết hạn gần nhất
        List<com.example.demo.entity.Batch> availableBatches = batchRepository.findAvailableBatchesOrderByExpiryAsc(warehouseId, productId);
        
        int remainingToDeduct = totalQuantityNeeded;
        for (com.example.demo.entity.Batch batch : availableBatches) {
            if (remainingToDeduct <= 0) break;

            int deductAmount = Math.min(batch.getQuantity(), remainingToDeduct);
            batch.decreaseQuantity(deductAmount);
            batchRepository.save(batch);
            
            remainingToDeduct -= deductAmount;
        }

        if (remainingToDeduct > 0) {
            // Lỗi logic (Database không nhất quán giữa Inventories và Batch)
            throw new RuntimeException("Lỗi FEFO: Không đủ hàng trong các lô để xuất!");
        }

        // Cập nhật Inventories
        inventory.decreaseStock(totalQuantityNeeded);
        inventoryRepository.save(inventory);
    }

    public List<Inventories> getAllInventory() {
        return inventoryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Inventories> getInventoryByWarehouse(Integer warehouseId) {
        return inventoryRepository.findByWarehouseId(warehouseId);
    }

    @Transactional(readOnly = true)
    public List<InventoryItemResponse> getInventoryItemsByWarehouse(Integer warehouseId) {
        return getInventoryByWarehouse(warehouseId).stream()
                .map(InventoryItemResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InventoryItemResponse> getAllInventoryItems() {
        return inventoryRepository.findAll().stream()
                .map(InventoryItemResponse::from)
                .toList();
    }
}