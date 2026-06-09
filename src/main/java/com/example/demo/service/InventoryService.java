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
    public void addStock(Integer warehouseId, Long productId, Integer amount) {

        InventoryId invId = new InventoryId(warehouseId.intValue(), productId);
        Inventories inventory = inventoryRepository.findById(invId).orElse(null);
        
        if (inventory != null) {
            inventory.increaseStock(amount); 
            inventoryRepository.save(inventory);
        } else {
            Warehouse warehouse = warehouseRepository.findById(warehouseId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy kho với id: " + warehouseId));
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với id: " + productId));

            Inventories newInventory = new Inventories();
            newInventory.setId(invId);
            newInventory.setWarehouse(warehouse);
            newInventory.setProduct(product);
            newInventory.setQuantity(amount);
            inventoryRepository.save(newInventory);
        }
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