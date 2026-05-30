package com.example.demo.service;

import com.example.demo.entity.*;
import com.example.demo.repository.*;
import com.example.demo.dto.request.OrderItemRequest;
import com.example.demo.dto.response.OrderResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryRepository inventoryRepository;
    private final WalletRepository walletRepository;
    private final CustomerRepository customerRepository;
    private final CustomerService customerService;
    private final RankRepository rankRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductPriceRepository productPriceRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;

    @Transactional
    public Order placeOrder(UUID customerId, Integer warehouseId, List<OrderItemRequest> itemRequests) {
        log.info("🚀 [BẮT ĐẦU ĐẶT HÀNG] Customer: {}, Warehouse: {}", customerId, warehouseId);

        if (customerId == null) {
            throw new IllegalArgumentException("customerId không được để trống");
        }
        if (warehouseId == null) {
            throw new IllegalArgumentException("warehouseId không được để trống");
        }
        if (itemRequests == null || itemRequests.isEmpty()) {
            throw new IllegalArgumentException("Giỏ hàng trống");
        }

        // 1. Kiểm tra thực thể cơ bản (customerId có thể là user id với tài khoản mới)
        Customer customer = customerService.resolveOrCreateCustomer(customerId);
        UUID resolvedCustomerId = customer.getId();
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new RuntimeException("Kho không tồn tại!"));
        Wallet wallet = walletRepository.findByCustomerId(resolvedCustomerId)
                .orElseThrow(() -> new RuntimeException("Khách hàng chưa có ví!"));

        Order order = Order.builder()
                .customer(customer)
                .warehouse(warehouse)
                .status(Order.OrderStatus.PENDING)
                .totalAmount(BigDecimal.ZERO)
                .orderDate(LocalDateTime.now())
                .items(new ArrayList<>())
                .build();

        BigDecimal totalOriginalAmount = BigDecimal.ZERO;

        for (OrderItemRequest req : itemRequests) {
            if (req.getProductId() == null) {
                throw new IllegalArgumentException("productId không hợp lệ");
            }
            if (req.getQuantity() == null || req.getQuantity() <= 0) {
                throw new IllegalArgumentException("Số lượng phải lớn hơn 0");
            }
            InventoryId invId = new InventoryId(warehouseId, req.getProductId());
            Inventories inventory = inventoryRepository.findById(invId)
                    .orElseThrow(() -> new RuntimeException("Sản phẩm ID " + req.getProductId() + " không có trong kho này!"));

            if (inventory.getQuantity() < req.getQuantity()) {
                throw new RuntimeException("Kho không đủ hàng cho sản phẩm: " + inventory.getProduct().getName());
            }

            // Lấy giá hiện hành
            BigDecimal effectivePrice = productPriceRepository.findCurrentPrice(req.getProductId(), LocalDateTime.now())
                    .map(ProductPrice::getPrice)
                    .orElse(inventory.getProduct().getBasePrice());

            // Trừ kho
            inventory.decreaseStock(req.getQuantity());
            inventoryRepository.save(inventory);

            // Tạo OrderItem
            OrderItem item = OrderItem.builder()
                    .order(order)
                    .product(inventory.getProduct())
                    .quantity(req.getQuantity())
                    .priceAtPurchase(effectivePrice)
                    .build();
            
            order.getItems().add(item);
            totalOriginalAmount = totalOriginalAmount.add(effectivePrice.multiply(BigDecimal.valueOf(req.getQuantity())));
        }

        // 4. Tính chiết khấu theo hạng
        BigDecimal discountRate = (customer.getRank() != null) ? customer.getRank().getDiscountRate() : BigDecimal.ZERO;
        BigDecimal discountAmount = totalOriginalAmount.multiply(discountRate).divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
        BigDecimal finalAmount = totalOriginalAmount.subtract(discountAmount);

        // 5. Thanh toán bằng ví
        if (wallet.getBalance().compareTo(finalAmount) < 0) {
            throw new RuntimeException("Ví không đủ tiền! Cần: " + finalAmount);
        }
        wallet.setBalance(wallet.getBalance().subtract(finalAmount));
        walletRepository.save(wallet);

        // Ghi log giao dịch
        TransactionHistory history = TransactionHistory.builder()
                .wallet(wallet)
                .amount(finalAmount.negate())
                .type("PAYMENT")
                .description("Thanh toán đơn hàng")
                .createdAt(LocalDateTime.now())
                .build();
        transactionHistoryRepository.save(history);

        // 6. Cập nhật thăng hạng
        BigDecimal newTotalSpent = (customer.getTotalSpent() != null ? customer.getTotalSpent() : BigDecimal.ZERO).add(finalAmount);
        customer.setTotalSpent(newTotalSpent);
        updateCustomerRank(customer);
        customerRepository.save(customer);

        // 7. Hoàn tất đơn hàng
        order.setTotalAmount(finalAmount);
        order.setStatus(Order.OrderStatus.PAID);
        return orderRepository.save(order);
    }

    @Transactional
    public void cancelOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng!"));

        if (!Order.OrderStatus.PAID.equals(order.getStatus())) {
            throw new RuntimeException("Chỉ có thể hủy đơn hàng đã thanh toán!");
        }

        // Hoàn kho
        for (OrderItem item : order.getItems()) {
            InventoryId invId = new InventoryId(order.getWarehouse().getId(), item.getProduct().getId());
            Inventories inventory = inventoryRepository.findById(invId)
                    .orElseThrow(() -> new RuntimeException("Lỗi dữ liệu kho!"));
            inventory.increaseStock(item.getQuantity());
            inventoryRepository.save(inventory);
        }

        // Hoàn tiền vào ví
        Wallet wallet = walletRepository.findByCustomerId(order.getCustomer().getId())
                .orElseThrow(() -> new RuntimeException("Lỗi ví!"));
        wallet.setBalance(wallet.getBalance().add(order.getTotalAmount()));
        walletRepository.save(wallet);

        // Ghi log hoàn tiền
        TransactionHistory history = TransactionHistory.builder()
                .wallet(wallet)
                .amount(order.getTotalAmount())
                .type("REFUND")
                .description("Hoàn tiền đơn hàng hủy: " + orderId)
                .createdAt(LocalDateTime.now())
                .build();
        transactionHistoryRepository.save(history);

        order.setStatus(Order.OrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    private void updateCustomerRank(Customer customer) {
        List<Rank> allRanks = rankRepository.findAllByOrderByMinPointDesc();
        int spentValue = customer.getTotalSpent().intValue();
        for (Rank r : allRanks) {
            if (spentValue >= r.getMinPoint()) {
                customer.setRank(r);
                break;
            }
        }
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAllWithDetails().stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByCustomerId(UUID customerId) {
        return orderRepository.findByCustomer_Id(customerId).stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional
    public OrderResponse updateOrderStatus(UUID orderId, String newStatus) {
        if (newStatus == null || newStatus.isBlank()) {
            throw new IllegalArgumentException("Trạng thái không hợp lệ");
        }
        String normalized = newStatus.trim().toUpperCase();
        List<String> allowed = List.of(
                "PENDING", "PAID", "PROCESSING", "SHIPPING", "DELIVERED", "COMPLETED", "CANCELLED");
        if (!allowed.contains(normalized)) {
            throw new IllegalArgumentException("Trạng thái không được hỗ trợ: " + newStatus);
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng!"));

        Order.OrderStatus targetStatus = Order.OrderStatus.valueOf(normalized);

        // Dùng == và != để so sánh Enum cực kỳ gọn gàng
        if (order.getStatus() == Order.OrderStatus.CANCELLED && targetStatus != Order.OrderStatus.CANCELLED) {
            throw new RuntimeException("Đơn đã hủy, không thể đổi trạng thái!");
        }

        order.setStatus(targetStatus);
        return OrderResponse.from(orderRepository.save(order));
    }
    
}