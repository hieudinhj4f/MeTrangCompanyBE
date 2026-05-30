package com.example.demo.repository;

import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    
    // Tìm kiếm nhân viên bằng username để đăng nhập
    Optional<User> findByUsername(String username);

    // Kiểm tra trùng lặp tài khoản khi tạo mới nhân viên
    boolean existsByUsername(String username);

    // Tìm kiếm nhanh theo email
    Optional<User> findByEmail(String email);
}