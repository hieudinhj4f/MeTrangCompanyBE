package com.example.demo.service;

import com.example.demo.entity.*;
import com.example.demo.repository.*;
import com.example.demo.dto.request.OrderItemRequest;
import com.example.demo.dto.request.OrderRequest;
import com.example.demo.dto.response.OrderResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryService inventoryService;
    private final WalletRepository walletRepository;
    private final CustomerRepository customerRepository;
    private final CustomerService customerService;
    private final WarehouseRepository warehouseRepository;
    private final ProductPriceRepository productPriceRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;
    private final ProductRepository productRepository;
    private final InvoiceRepository invoiceRepository;

    @Transactional

    public Order placeOrder(UUID customerId, OrderRequest request) {
        log.info("🚀 [BẮT ĐẦU ĐẶT HÀNG ĐA KÊNH] Customer: {}, Warehouse: {}, Payment: {}, Type: {}", customerId, request.getWarehouseId(), request.getPaymentMethod(), request.getOrderType());

        if (customerId == null) throw new IllegalArgumentException("customerId không được để trống");
        if (request.getWarehouseId() == null) throw new IllegalArgumentException("warehouseId không được để trống");
        if (request.getItems() == null || request.getItems().isEmpty()) throw new IllegalArgumentException("Giỏ hàng trống");
        
        Order.PaymentMethod paymentMethod = request.getPaymentMethod() != null ? 
                Order.PaymentMethod.valueOf(request.getPaymentMethod()) : Order.PaymentMethod.CASH;
        Order.OrderType orderType = request.getOrderType() != null ? 
                Order.OrderType.valueOf(request.getOrderType()) : Order.OrderType.IN_STORE;


        // 1. Kiểm tra thực thể cơ bản
        Customer customer = customerService.resolveOrCreateCustomer(customerId);
        UUID resolvedCustomerId = customer.getId();
        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new RuntimeException("Kho không tồn tại!"));
        

        Customer.CustomerType customerType = customer.getCustomerType() != null ? customer.getCustomerType() : Customer.CustomerType.RETAIL;

        Order order = Order.builder()
                .customer(customer)
                .warehouse(warehouse)
                .paymentMethod(paymentMethod) 
                .orderType(orderType)
                .deliveryAddress(request.getDeliveryAddress())
                .status(Order.OrderStatus.PENDING)
                .totalAmount(BigDecimal.ZERO)
                .orderDate(LocalDateTime.now())
                .items(new ArrayList<>())
                .build();

        BigDecimal totalOriginalAmount = BigDecimal.ZERO;

        // 2. Trừ kho và tính tổng tiền gốc (Giữ nguyên logic cực chuẩn của bạn)
        for (OrderItemRequest req : request.getItems()) {
            if (req.getProductId() == null) throw new IllegalArgumentException("productId không hợp lệ");
            if (req.getQuantity() == null || req.getQuantity() <= 0) throw new IllegalArgumentException("Số lượng phải lớn hơn 0");
            
            Product product = productRepository.findById(req.getProductId())
                    .orElseThrow(() -> new RuntimeException("Sản phẩm ID " + req.getProductId() + " không tồn tại!"));
            
            // Tự động trừ kho (có FEFO - trừ lô cũ nhất) NẾU SẢN PHẨM LÀ NGUYÊN LIỆU (B2B)
            if (Boolean.TRUE.equals(product.getIsIngredient())) {
                if (request.getWarehouseId() == null) {
                    throw new IllegalArgumentException("Vui lòng chọn Kho xuất hàng khi bán Nguyên liệu!");
                }
                inventoryService.exportIngredient(request.getWarehouseId(), product.getId(), req.getQuantity());
            }

            BigDecimal effectivePrice = product.getBasePrice();

            OrderItem item = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(req.getQuantity())
                    .priceAtPurchase(effectivePrice)
                    .build();
            
            order.getItems().add(item);
            totalOriginalAmount = totalOriginalAmount.add(effectivePrice.multiply(BigDecimal.valueOf(req.getQuantity())));
        }

        // THAY ĐỔI 2: ĐỊNH GIÁ ĐỘNG (Dynamic Pricing)
        BigDecimal finalAmount = totalOriginalAmount;

        if (customerType == Customer.CustomerType.ENTERPRISE) {
            // Khách B2B: Áp dụng chiết khấu riêng hoặc mặc định 15%
            BigDecimal discountRate = customer.getB2bDiscountRate() != null && customer.getB2bDiscountRate().compareTo(BigDecimal.ZERO) > 0 
                ? customer.getB2bDiscountRate().divide(new BigDecimal("100")) 
                : new BigDecimal("0.15");
            BigDecimal discountAmount = finalAmount.multiply(discountRate).setScale(0, RoundingMode.HALF_UP);
            finalAmount = finalAmount.subtract(discountAmount);
        } 
        else if (customerType == Customer.CustomerType.WORKER) {
            // Khách Công nhân: Trợ giá cố định giảm 5.000đ
            finalAmount = finalAmount.subtract(new BigDecimal("5000")).max(BigDecimal.ZERO);
        } 
        else {
            // Khách Vãng lai (RETAIL): Mặc định không chiết khấu
            finalAmount = totalOriginalAmount;
        }

        // THAY ĐỔI 3: PHÂN LUỒNG THANH TOÁN (Payment Routing)
        if (paymentMethod == Order.PaymentMethod.WALLET) {
            Wallet wallet = walletRepository.findByCustomerId(resolvedCustomerId)
                    .orElseThrow(() -> new RuntimeException("Khách hàng chưa kích hoạt ví điện tử!"));
            
            // Tự động nạp tiền mặt phần còn thiếu nếu ví không đủ tiền
            BigDecimal missingAmount = finalAmount.subtract(wallet.getBalance());
            if (missingAmount.compareTo(BigDecimal.ZERO) > 0) {
                if (Boolean.TRUE.equals(request.getIsOnlineOrder())) {
                    throw new RuntimeException("Ví không đủ tiền! Vui lòng nạp thêm qua cổng thanh toán hoặc chọn phương thức Tiền mặt khi nhận hàng.");
                }

                // Nếu mua tại quầy -> Auto nạp bù bằng tiền mặt
                wallet.setBalance(wallet.getBalance().add(missingAmount));
                
                TransactionHistory topupHistory = TransactionHistory.builder()
                        .wallet(wallet)
                        .amount(missingAmount)
                        .type("CASH_TOPUP")
                        .description("Khách nạp bù tiền mặt cho đơn hàng")
                        .createdAt(LocalDateTime.now())
                        .build();
                transactionHistoryRepository.save(topupHistory);
            }

            // Trừ tiền thanh toán từ ví
            wallet.setBalance(wallet.getBalance().subtract(finalAmount));
            walletRepository.save(wallet);

            TransactionHistory history = TransactionHistory.builder()
                    .wallet(wallet)
                    .amount(finalAmount.negate())
                    .type("PAYMENT")
                    .description("Thanh toán đơn hàng qua ví nội bộ")
                    .createdAt(LocalDateTime.now())
                    .build();
            transactionHistoryRepository.save(history);
            
            order.setStatus(Order.OrderStatus.PAID);
        } 
        else if (paymentMethod == Order.PaymentMethod.DEBT) {
            if (customerType != Customer.CustomerType.ENTERPRISE) {
                throw new IllegalArgumentException("Chỉ khách hàng Doanh nghiệp mới được ghi nhận công nợ!");
            }
            

            BigDecimal currentDebt = orderRepository.sumUnpaidDebtByCustomer(resolvedCustomerId);
            BigDecimal limit = customer.getCreditLimit() != null ? customer.getCreditLimit() : BigDecimal.ZERO;
            BigDecimal projectedDebt = currentDebt.add(finalAmount); // Nợ cũ + Đơn mới
            
            if (projectedDebt.compareTo(limit) > 0) {
                throw new IllegalArgumentException("Từ chối giao dịch! Vượt Hạn mức công nợ. (Nợ hiện tại: " 
                        + currentDebt.longValue() + "đ, Hạn mức: " + limit.longValue() + "đ)");
            }

            order.setStatus(Order.OrderStatus.PROCESSING); 
        }
        else { // CASH hoặc Chuyển khoản QR
            order.setStatus(Order.OrderStatus.PAID);
        }

        // 6. Cập nhật thăng hạng (Áp dụng cho mọi        // Add Reward Points if needed in the future
        // customerRepository.save(customer);

        // 7. Hoàn tất lưu đơn hàng
        order.setTotalAmount(finalAmount);
        Order savedOrder = orderRepository.save(order);

        // 8. Tạo Yêu cầu Hóa Đơn (Nếu có)
        if (Boolean.TRUE.equals(request.getRequiresInvoice())) {
            Invoice invoice = Invoice.builder()
                    .order(savedOrder)
                    .companyName(request.getCompanyName() != null && !request.getCompanyName().isBlank() 
                            ? request.getCompanyName() 
                            : (customer.getCompanyName() != null ? customer.getCompanyName() : customer.getFullName()))
                    .taxCode(request.getTaxCode() != null && !request.getTaxCode().isBlank() 
                            ? request.getTaxCode() 
                            : customer.getTaxCode())
                    .billingAddress(request.getBillingAddress() != null && !request.getBillingAddress().isBlank() 
                            ? request.getBillingAddress() 
                            : (customer.getBillingAddress() != null ? customer.getBillingAddress() : "Chưa cập nhật"))
                    .isIssued(false)
                    .build();
            invoiceRepository.save(invoice);
        }

        return savedOrder;
    }

    @Transactional
    public void cancelOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng!"));

        if (order.getStatus() == Order.OrderStatus.CANCELLED) {
            throw new RuntimeException("Đơn hàng này đã được hủy trước đó!");
        }

        // Hoàn kho (với lô trả hàng)
        if (order.getWarehouse() != null) {
            for (OrderItem item : order.getItems()) {
                inventoryService.addStockWithBatch(
                    order.getWarehouse().getId(),
                    item.getProduct().getId(),
                    item.getQuantity(),
                    "REFUND-" + order.getId().toString().substring(0, 8).toUpperCase(),
                    LocalDateTime.now().plusMonths(6).toLocalDate()
                );
            }
        }

        // THAY ĐỔI 4: Chỉ hoàn tiền vào ví NẾU khách đã thanh toán bằng ví
        if (order.getPaymentMethod() == Order.PaymentMethod.WALLET) {
            Wallet wallet = walletRepository.findByCustomerId(order.getCustomer().getId())
                    .orElseThrow(() -> new RuntimeException("Lỗi ví khi hoàn tiền!"));
            wallet.setBalance(wallet.getBalance().add(order.getTotalAmount()));
            walletRepository.save(wallet);

            TransactionHistory history = TransactionHistory.builder()
                    .wallet(wallet)
                    .amount(order.getTotalAmount())
                    .type("REFUND")
                    .description("Hoàn tiền đơn hàng hủy: " + orderId)
                    .createdAt(LocalDateTime.now())
                    .build();
            transactionHistoryRepository.save(history);
        }
        // Nếu thanh toán CASH hoặc DEBT thì nhân viên tự xử lý tiền mặt/công nợ bên ngoài.

        order.setStatus(Order.OrderStatus.CANCELLED);
        orderRepository.save(order);
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
        if (newStatus == null || newStatus.isBlank()) throw new IllegalArgumentException("Trạng thái không hợp lệ");
        
        String normalized = newStatus.trim().toUpperCase();
        List<String> allowed = List.of("PENDING", "PAID", "PROCESSING", "SHIPPING", "DELIVERED", "COMPLETED", "CANCELLED");
        
        if (!allowed.contains(normalized)) {
            throw new IllegalArgumentException("Trạng thái không được hỗ trợ: " + newStatus);
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng!"));

        Order.OrderStatus targetStatus = Order.OrderStatus.valueOf(normalized);

        if (order.getStatus() == Order.OrderStatus.CANCELLED && targetStatus != Order.OrderStatus.CANCELLED) {
            throw new RuntimeException("Đơn đã hủy, không thể đổi trạng thái!");
        }

        order.setStatus(targetStatus);
        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse updateOrderPriority(UUID orderId, Boolean isPriority) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng!"));
        
        order.setIsPriority(isPriority != null && isPriority);
        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public List<Object[]> getRevenueByPaymentMethod(LocalDateTime startDate, LocalDateTime endDate) {
        return orderRepository.getRevenueByPaymentMethod(startDate, endDate);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getKitchenSummary(LocalDateTime startDate, LocalDateTime endDate) {
        List<Object[]> rawData = orderRepository.getKitchenSummary(startDate, endDate);
        return rawData.stream().map(row -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("productName", row[0]);
            map.put("totalQuantity", row[1]);
            return map;
        }).collect(Collectors.toList());
    }
}