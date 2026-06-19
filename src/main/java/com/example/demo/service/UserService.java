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

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Tìm kiếm bằng UUID
    public Optional<User> getUserById(UUID id) {
        return userRepository.findById(id);
    }

    // Lưu nhân viên / khách hàng
    @Transactional
    public User saveUser(User user, UUID enterpriseId) {
        User saved = userRepository.save(user);
        if (saved.getRole() == Role.CUSTOMER) {
            com.example.demo.entity.Customer customer = customerService.ensureCustomerForUser(saved);
            if (enterpriseId != null) {
                customer.setEnterpriseId(enterpriseId);
            }
            return userRepository.findById(saved.getId()).orElse(saved);
        }
        return saved;
    }

    public void deactivateUser(UUID id) {
        userRepository.findById(id).ifPresent(user -> {
            user.setIsActive(false);
            userRepository.save(user);
        });
    }
}