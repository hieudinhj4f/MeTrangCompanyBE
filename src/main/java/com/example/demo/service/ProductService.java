package com.example.demo.service;

import com.example.demo.dto.request.QuickAddProductRequest;
import com.example.demo.dto.response.ProductResponse;
import com.example.demo.entity.Product;
import com.example.demo.entity.ProductPrice;
import com.example.demo.repository.ProductRepository;
import com.example.demo.dto.request.PriceConfigRequest;
import com.example.demo.dto.request.ProductRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.demo.entity.Category;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.ProductPriceRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductPriceRepository productPriceRepository;
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
                .isIngredient(ingredient)
                .active(request.getActive() != null ? request.getActive() : true)
                .category(request.getCategoryId() != null ? categoryRepository.findById(request.getCategoryId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục với ID: " + request.getCategoryId())) : null)
                .isBestSeller(false)
                .imageUrl(request.getImageUrl() != null ? request.getImageUrl().trim() : null)
                .build();

        Product savedProduct = saveProduct(product);

        // Lưu vết giá khởi tạo
        ProductPrice initialPrice = ProductPrice.builder()
                .product(savedProduct)
                .price(savedProduct.getBasePrice())
                .priceType("REGULAR")
                .startDate(LocalDateTime.now())
                .description("Khởi tạo giá sản phẩm")
                .build();
        productPriceRepository.save(initialPrice);

        return savedProduct;
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
        BigDecimal newPrice = BigDecimal.valueOf(request.getBasePrice());
        // So sánh giá cũ và mới
        if (product.getBasePrice() == null || product.getBasePrice().compareTo(newPrice) != 0) {
            product.setBasePrice(newPrice);
            // Lưu vết lịch sử giá
            ProductPrice priceHistory = ProductPrice.builder()
                    .product(product)
                    .price(newPrice)
                    .priceType("REGULAR")
                    .startDate(LocalDateTime.now())
                    .description("Cập nhật giá sản phẩm")
                    .build();
            productPriceRepository.save(priceHistory);
        }
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
    
        return productRepository.save(product);
    }
    @Transactional
    public ProductPrice addPriceConfiguration(Long productId, PriceConfigRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

        // Validate logic nghiệp vụ
        if ("EVENT".equals(request.getPriceType()) && request.getEndDate() == null) {
            throw new IllegalArgumentException("Giá sự kiện bắt buộc phải có Ngày kết thúc (endDate)!");
        }

        ProductPrice newPrice = ProductPrice.builder()
                .product(product)
                .price(request.getPrice())
                .priceType(request.getPriceType())
                .startDate(request.getStartDate() != null ? request.getStartDate() : LocalDateTime.now())
                .endDate(request.getEndDate())
                .description(request.getDescription())
                .build();

        ProductPrice savedPrice = productPriceRepository.save(newPrice);

        // Cập nhật base_price
        product.setBasePrice(request.getPrice());
        productRepository.save(product); 

        return savedPrice;
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));
        
        try {
            productPriceRepository.deleteByProductId(product.getId()); // Optional: clean up prices if needed
            productRepository.delete(product);
        } catch (Exception e) {
            throw new RuntimeException("Không thể xóa sản phẩm này vì đã phát sinh giao dịch (tồn kho, đơn hàng...). Hãy sử dụng chức năng Ngưng Kinh Doanh thay thế!");
        }
    }
}
