package com.example.demo.controller;

import com.example.demo.dto.request.QuickAddProductRequest;
import com.example.demo.dto.request.ProductRequest;
import com.example.demo.dto.response.ProductResponse;
import com.example.demo.entity.Product;
import com.example.demo.entity.ProductPrice;
import com.example.demo.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProductResponses());
    }

    @GetMapping("/active")
    public ResponseEntity<List<ProductResponse>> getActiveProducts() {
        return ResponseEntity.ok(productService.getActiveProductResponses());
    }


    @PostMapping("/quick-add")
    public ResponseEntity<?> quickAddProduct(@RequestBody QuickAddProductRequest request) {
        try {
            Product saved = productService.quickAddProduct(request);
            return ResponseEntity.ok(ProductResponse.from(saved));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "Thất bại",
                    "reason", e.getMessage()));
        }
    }

    /**
     * Chi tiết sản phẩm — khai báo sau /active và /quick-add để tránh nhầm path
     * GET /api/products/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getProductById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(productService.getProductResponseById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "Không tìm thấy",
                    "reason", e.getMessage()));
        }
    }

    @PostMapping("/{id}/toggle-active")
    public ResponseEntity<?> updateEntity(@PathVariable Long id) {
        try {
            Product updated = productService.getProductById(id);
            if (updated == null) {
                throw new RuntimeException("Sản phẩm không tồn tại");
            }
                updated.setActive(!updated.getActive());
                productService.saveProduct(updated);
            return ResponseEntity.ok(ProductResponse.from(updated));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "Không tìm thấy",
                    "reason", e.getMessage()));

        }  
    }

    // 💡 Đây mới là API nhận dữ liệu từ Modal chỉnh sửa của React gửi xuống
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProductDetails(@PathVariable Long id, @RequestBody ProductRequest request) {
        try {
            // Gọi hàm xử lý cập nhật ở tầng Service
            Product updatedProduct = productService.updateProductDetails(id, request);
            return ResponseEntity.ok(Map.of(
                "status", "Thành công",
                "data", updatedProduct
            ));
        } catch (RuntimeException e) {
            // Bắt lỗi (ví dụ: trùng SKU) và báo về cho React hiển thị message màu đỏ
            return ResponseEntity.badRequest().body(Map.of(
                "status", "Lỗi",
                "reason", e.getMessage()
            ));
        }
    }
    @PostMapping("/{id}/prices")
    public ResponseEntity<?> configureProductPrice(
            @PathVariable Long id, 
            @RequestBody com.example.demo.dto.request.PriceConfigRequest request) {
        try {
            // (Nếu cần, thêm assertAdminAccess() ở đây để chặn nhân viên sửa giá)
            ProductPrice savedPrice = productService.addPriceConfiguration(id, request);
            return ResponseEntity.ok(Map.of(
                    "status", "Thành công",
                    "message", "Đã thiết lập cấu hình giá mới!",
                    "data", savedPrice
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("status", "Lỗi", "reason", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status", "Lỗi", "reason", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        try {
            productService.deleteProduct(id);
            return ResponseEntity.ok(Map.of("status", "Thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("reason", e.getMessage()));
        }
    }
}
