package com.example.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {
    //id tự tăng
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Tên danh mục bắt buộc, không được trùng
    @Column(name = "category_name", nullable = false, unique = true)
    private String categoryName;

    // Trạng thái hoạt động
    @Column(name = "is_active")
    private Boolean isActive;

    //quan hệ 1-n với product 
    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Product> products;
}