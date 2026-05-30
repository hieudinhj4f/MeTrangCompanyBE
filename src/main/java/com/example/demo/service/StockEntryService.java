package com.example.demo.service;

import com.example.demo.entity.*;
import com.example.demo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class StockEntryService {

    private final StockEntryRepository stockEntryRepository;
    private final InventoryRepository inventoryRepository;

    @Transactional
    public StockEntry createStockEntry(StockEntry stockEntry) {

        StockEntry savedEntry = stockEntryRepository.save(stockEntry);

        // 2. Cập nhật kho cho từng nguyên liệu/sản phẩm trong phiếu
        for (StockEntryItem item : savedEntry.getItems()) {
            InventoryId invId = new InventoryId(
                savedEntry.getWarehouse().getId(),
                item.getProduct().getId()
            );

            // Tìm bản ghi kho, nếu chưa có thì tạo mới
            Inventories inventory = inventoryRepository.findById(invId)
                .orElse(Inventories.builder()
                    .id(invId)
                    .warehouse(savedEntry.getWarehouse())
                    .product(item.getProduct())
                    .quantity(0)
                    .build());

            inventory.increaseStock(item.getQuantity());
            inventoryRepository.save(inventory);
        }
        return savedEntry;
    }
}