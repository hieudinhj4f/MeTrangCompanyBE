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

    /**
     * Điều chỉnh số lượng kho thủ công (Nhập/Xuất)
     */
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

    /**
     * 💡 HÀM ĐÃ ĐƯỢC TỐI ƯU HÓA: Tự động khởi tạo tồn kho nếu sản phẩm mới tinh chưa có bản ghi
     */
    @Transactional
    public void addStock(Integer warehouseId, Long productId, Integer amount) {
        // 1. Khởi tạo khóa chính phức hợp (Ép kiểu từ Long sang Integer nếu class InventoryId của bạn dùng Integer)
        InventoryId invId = new InventoryId(warehouseId.intValue(), productId);
        
        // 2. Tìm kiếm bản ghi tồn kho dựa trên cặp khóa chính phức hợp
        Inventories inventory = inventoryRepository.findById(invId).orElse(null);
        
        if (inventory != null) {
            // 🔄 TÌNH HUỐNG 1: Sản phẩm đã từng lưu kho -> Dùng hàm tăng số lượng nguyên bản của Hiếu
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