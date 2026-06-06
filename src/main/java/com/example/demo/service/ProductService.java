package com.example.demo.service;

import com.example.demo.dto.request.QuickAddProductRequest;
import com.example.demo.dto.response.ProductResponse;
import com.example.demo.entity.Product;
import com.example.demo.repository.ProductRepository;
import com.example.demo.dto.request.ProductRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.demo.entity.Category;
import com.example.demo.repository.CategoryRepository;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProductResponses() {
        return productRepository.findAll().stream()
                .map(ProductResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getActiveProductResponses() {
        return productRepository.findByActiveTrue().stream()
                .map(ProductResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductResponseById(Long id) {
        return ProductResponse.from(getProductById(id));
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public List<Product> getProductsByCategory(Integer categoryId) {
        return productRepository.findByCategoryId(categoryId);
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm!"));
    }

    public List<Product> getAllActiveProducts() {
        return productRepository.findByActiveTrue();
    }

    @Transactional
    public Product saveProduct(Product product) {
        if (productRepository.existsBySku(product.getSku())) {
            throw new RuntimeException("Mã SKU đã tồn tại, vui lòng chọn mã khác!");
        }
        if (product.getBasePrice() == null) {
            product.setBasePrice(BigDecimal.ZERO);
        }
        if (product.getActive() == null) {
            product.setActive(true);
        }
        return productRepository.save(product);
    }

    @Transactional
    public Product quickAddProduct(QuickAddProductRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new RuntimeException("Tên vật phẩm không được để trống!");
        }
        if (request.getSku() == null || request.getSku().isBlank()) {
            throw new RuntimeException("Mã SKU không được để trống!");
        }

        boolean ingredient = request.getIsIngredient() != null
                ? request.getIsIngredient()
                : isIngredientUnit(request.getUnit());

        Product product = Product.builder()
                .name(request.getName().trim())
                .sku(request.getSku().trim())
                .basePrice(request.getBasePrice() != null ? request.getBasePrice() : BigDecimal.ZERO)
                .salePrice(request.getSalePrice() != null ? request.getSalePrice() : BigDecimal.ZERO)
                .isIngredient(ingredient)
                .active(request.getActive() != null ? request.getActive() : true)
                .category(request.getCategoryId() != null ? categoryRepository.findById(request.getCategoryId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục với ID: " + request.getCategoryId())) : null)
                .isBestSeller(false)
                .imageUrl(request.getImageUrl() != null ? request.getImageUrl().trim() : null)
                .build();

        return saveProduct(product);
    }

    private boolean isIngredientUnit(String unit) {
        if (unit == null) {
            return false;
        }
        String normalized = unit.trim().toLowerCase();
        return normalized.equals("kg") || normalized.equals("g") || normalized.equals("lít") || normalized.equals("lit");
    }
    
    @Transactional
    public Product updateProductDetails(Long id, ProductRequest request) {
        // 1. Tìm sản phẩm theo ID xem có tồn tại không
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại trong hệ thống!"));

        // 2. 💡 XỬ LÝ LỖI TRÙNG SKU THÔNG MINH
        // Nếu mã SKU gửi lên KHÁC mã SKU hiện tại của sản phẩm, thì mới đi kiểm tra xem có trùng với sản phẩm khác không
        if (!product.getSku().equals(request.getSku()) && productRepository.existsBySku(request.getSku())) {
            throw new RuntimeException("Mã SKU này đã được sử dụng cho một sản phẩm khác!");
        }

        // 3. Cập nhật các thông tin mới từ Form React
       // 1. Ánh xạ các trường chuỗi và boolean (Giống nhau 100%)
    product.setName(request.getName());
    product.setSku(request.getSku());
    product.setDescription(request.getDescription());
    product.setIsIngredient(request.getIsIngredient());

    // 2. 💡 XỬ LÝ LỆCH KIỂU GIÁ TIỀN (Double -> BigDecimal)
    // Phải bọc BigDecimal.valueOf() và kiểm tra null để tránh lỗi NullPointerException
    if (request.getBasePrice() != null) {
        product.setBasePrice(BigDecimal.valueOf(request.getBasePrice()));
    }
    if (request.getSalePrice() != null) {
        product.setSalePrice(BigDecimal.valueOf(request.getSalePrice()));
    } else {
        // Nếu không có giá sale, có thể set null hoặc tự động set bằng 0 tùy logic của bạn
        product.setSalePrice(BigDecimal.ZERO); 
    }

    // 3. 💡 XỬ LÝ LỆCH KIỂU DANH MỤC (Integer -> Đối tượng Category)
    if (request.getCategoryId() != null) {
        // Ép kiểu Integer sang Long (nếu ID bảng Category của bạn dùng kiểu Long)
        Integer catId = request.getCategoryId();
        
        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục với ID: " + catId));
        product.setCategory(category);
    }

    // 4. Cập nhật hình ảnh (Chỉ cập nhật nếu có ảnh mới gửi lên)
    if (request.getImageUrl() != null && !request.getImageUrl().trim().isEmpty()) {
        product.setImageUrl(request.getImageUrl());
    }
        
        // Nếu người dùng có upload ảnh mới (link Cloudinary) thì mới cập nhật, không thì giữ nguyên ảnh cũ
        if (request.getImageUrl() != null && !request.getImageUrl().isEmpty()) {
            product.setImageUrl(request.getImageUrl());
        }

        // Nếu DTO của bạn có thêm trường discountPrice thì bổ sung vào đây:
        // product.setDiscountPrice(request.getDiscountPrice());

        // 4. Lưu đè xuống DB
        return productRepository.save(product);
    }
}
