package com.example.demo.service;

import com.example.demo.entity.User;
import com.example.demo.entity.User.Role;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerService customerService;

    // Lấy danh sách nhân viên
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Tìm kiếm bằng UUID
    public Optional<User> getUserById(UUID id) { 
        return userRepository.findById(id);
    }

    // Lưu nhân viên / khách hàng
    @Transactional
    public User saveUser(User user) {
        User saved = userRepository.save(user);
        if (saved.getRole() == Role.CUSTOMER) {
            customerService.ensureCustomerForUser(saved);
            return userRepository.findById(saved.getId()).orElse(saved);
        }
        return saved;
    }

    // Vô hiệu hóa tài khoản (Soft Delete)
    public void deactivateUser(UUID id) {
        userRepository.findById(id).ifPresent(user -> {
            user.setIsActive(false); // Đảm bảo Entity dùng isActive thay vì status
            userRepository.save(user);
        });
    }
}