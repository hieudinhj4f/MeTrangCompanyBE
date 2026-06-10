package com.example.demo.service;

import com.example.demo.dto.request.ReceiptRequest; // Import DTO nhận dữ liệu từ form Frontend
import com.example.demo.dto.response.InventoryItemResponse;
import com.example.demo.dto.response.WarehouseResponse;
import com.example.demo.dto.response.WarehouseTransactionResponse;
import com.example.demo.entity.*;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.StockEntryRepository;
import com.example.demo.repository.WarehouseRepository;
import com.example.demo.repository.ProductRepository; // Cần dùng để tìm Product Entity
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final InventoryService inventoryService;
    private final StockEntryRepository stockEntryRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository; // Thêm repository này để truy vấn sản phẩm real

    public List<WarehouseResponse> getAllWarehouses() {
        return warehouseRepository.findAll().stream()
                .map(WarehouseResponse::from)
                .toList();
    }

    public WarehouseResponse getWarehouseById(Integer id) {
        return WarehouseResponse.from(findWarehouseEntity(id));
    }

    public List<InventoryItemResponse> getInventoryByWarehouse(Integer warehouseId) {
        findWarehouseEntity(warehouseId);
        return inventoryService.getInventoryItemsByWarehouse(warehouseId);
    }

    private Warehouse findWarehouseEntity(Integer id) {
        return warehouseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy kho với id: " + id));
    }

    @Transactional(readOnly = true)
    public List<WarehouseTransactionResponse> getTransactionsByWarehouse(Integer warehouseId) {
        findWarehouseEntity(warehouseId);
        List<WarehouseTransactionResponse> transactions = new ArrayList<>();

        for (StockEntry entry : stockEntryRepository.findByWarehouseIdWithItems(warehouseId)) {
            if (entry.getItems() == null) continue;
            for (StockEntryItem item : entry.getItems()) {
                if (item.getProduct() == null) continue;
                transactions.add(WarehouseTransactionResponse.builder()
                        .createdAt(entry.getEntryDate())
                        .type("IMPORT")
                        .productName(item.getProduct().getName())
                        .productSku(item.getProduct().getSku())
                        .quantity(item.getQuantity())
                        .staffName(entry.getSupplier() != null ? entry.getSupplier().getName() : "Nhập kho")
                        .build());
            }
        }

        for (Order order : orderRepository.findByWarehouseIdWithItems(warehouseId)) {
            if (order.getItems() == null) continue;
            String staffName = order.getCustomer() != null ? order.getCustomer().getFullName() : "Xuất kho";
            for (OrderItem item : order.getItems()) {
                if (item.getProduct() == null) continue;
                transactions.add(WarehouseTransactionResponse.builder()
                        .createdAt(order.getOrderDate())
                        .type("EXPORT")
                        .productName(item.getProduct().getName())
                        .productSku(item.getProduct().getSku())
                        .quantity(item.getQuantity())
                        .staffName(staffName)
                        .build());
            }
        }

        transactions.sort(Comparator.comparing(
                WarehouseTransactionResponse::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return transactions;
    }


    @Transactional
    public void createImportStockEntry(ReceiptRequest request) {
        Warehouse warehouse = findWarehouseEntity(request.getWarehouseId());

        StockEntry stockEntry = new StockEntry();
        stockEntry.setWarehouse(warehouse);
        stockEntry.setEntryDate(request.getCreatedDate() != null ? request.getCreatedDate() : LocalDateTime.now());

        stockEntry.setIsApproved(false); 

        List<StockEntryItem> entryItems = new ArrayList<>();
        for (ReceiptRequest.ItemDetail itemDto : request.getItems()) {
            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + itemDto.getProductId()));

            StockEntryItem entryItem = new StockEntryItem();
            entryItem.setStockEntry(stockEntry);
            entryItem.setProduct(product);
            entryItem.setQuantity(itemDto.getQuantity());
            
            if (itemDto.getPrice() != null && itemDto.getPrice() > 0) {
                entryItem.setPurchasePrice(java.math.BigDecimal.valueOf(itemDto.getPrice()));
            }
            if (itemDto.getBatchCode() != null) {
                entryItem.setBatchCode(itemDto.getBatchCode());
            }
            if (itemDto.getExpiryDate() != null) {
                entryItem.setExpiryDate(itemDto.getExpiryDate());
            }

            entryItems.add(entryItem);
            // ĐÃ XÓA dòng inventoryService.addStock ở đây để đảm bảo an toàn dữ liệu
        }

        stockEntry.setItems(entryItems);
        stockEntryRepository.save(stockEntry);
    }

    @Transactional
    public void approveReceivedStock(Integer warehouseId, Long receiptId) {
        StockEntry entry = stockEntryRepository.findByIdWithWarehouse(receiptId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu nhập với id: " + receiptId));
        
        if (!entry.getWarehouse().getId().equals(warehouseId)) {
            throw new RuntimeException("Phiếu nhập không thuộc kho này");
        }
        if (entry.getIsApproved()) {
            throw new RuntimeException("Phiếu nhập đã được duyệt rồi, không thể duyệt lại!");
        }

        // BƯỚC QUYẾT ĐỊNH: Chỉ cộng số lượng vào kho khi Quản lý bấm nút Phê Duyệt
        for (StockEntryItem item : entry.getItems()) {
            if (item.getBatchCode() == null || item.getExpiryDate() == null) {
                throw new RuntimeException("Sản phẩm " + item.getProduct().getName() + " thiếu mã lô hoặc ngày hết hạn!");
            }
            inventoryService.addStockWithBatch(warehouseId, item.getProduct().getId(), item.getQuantity(), item.getBatchCode(), item.getExpiryDate());
        }

        // Đổi trạng thái phiếu thành Đã duyệt
        entry.setIsApproved(true);
        stockEntryRepository.save(entry);
    }
}