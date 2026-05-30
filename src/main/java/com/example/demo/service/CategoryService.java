package com.example.demo.service;

import com.example.demo.entity.Category;
import com.example.demo.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    // 2. Lấy chi tiết 1 danh mục theo ID
    public Category getCategoryById(Integer id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục với ID: " + id));
    }

    // 3. Thêm mới danh mục
    public Category createCategory(Category category) {
        return categoryRepository.save(category);
    }

    // 4. Cập nhật thông tin danh mục
    public Category updateCategory(Integer id, Category categoryDetails) {
        Category existingCategory = getCategoryById(id);

        existingCategory.setCategoryName(categoryDetails.getCategoryName());
        
        return categoryRepository.save(existingCategory);
    }

    public void deleteCategory(Integer id) {
        // Kiểm tra xem danh mục có tồn tại không trước khi xóa
        Category existingCategory = getCategoryById(id);
    
        categoryRepository.delete(existingCategory);
    }
}