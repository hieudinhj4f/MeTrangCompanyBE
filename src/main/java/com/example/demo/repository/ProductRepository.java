package com.example.demo.repository;

import com.example.demo.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Tìm danh sách sản phẩm dựa trên ID của danh mục (Cafe, Trà, Bánh...)
    List<Product> findByCategoryId(Integer categoryId);

    // Tìm kiếm sản phẩm theo tên (hỗ trợ làm thanh tìm kiếm trên Frontend)
    List<Product> findByNameContainingIgnoreCase(String name);

    // Lấy danh sách các sản phẩm đang là Best Seller
    List<Product> findByIsBestSellerTrue();

    List<Product> findByActiveTrue();

    // Kiểm tra xem SKU đã tồn tại chưa khi thêm sản phẩm mới
    boolean existsBySku(String sku);

    // Lấy danh sách chỉ gồm các nguyên liệu
    List<Product> findByIsIngredientTrue();

    // Lấy danh sách sản phẩm thực tế để bán (không bao gồm nguyên liệu)
    List<Product> findByIsIngredientFalse();
}