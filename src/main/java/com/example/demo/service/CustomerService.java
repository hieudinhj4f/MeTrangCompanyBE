package com.example.demo.service;

import com.example.demo.entity.Customer;
import com.example.demo.entity.Customer.CustomerType;
import com.example.demo.entity.User;
import com.example.demo.entity.Wallet;
import com.example.demo.repository.CustomerRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.Optional;
import java.util. List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;

    @Transactional
    public UUID ensureCustomerForUserId(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản: " + userId));
        return ensureCustomerForUser(user).getId();
    }

    @Transactional
public Customer ensureCustomerForUser(User user) {
    if (user.getCustomer() != null) {
        ensureWalletExists(user.getCustomer());
        return user.getCustomer();
    }

    Customer customer = Customer.builder()
            .fullName(user.getFullName() != null ? user.getFullName() : user.getUsername())
            .email(user.getEmail())
            .phoneNumber(user.getPhone())
            .totalSpent(BigDecimal.ZERO)
            .customerType(CustomerType.RETAIL)
            .build();
            
    customer = customerRepository.saveAndFlush(customer);

    ensureWalletExists(customer);

    user.setCustomer(customer);
    userRepository.save(user);
    return customer;
}


    @Transactional
    public Customer resolveOrCreateCustomer(UUID id) {
        return customerRepository.findById(id)
                .orElseGet(() -> userRepository.findById(id)
                        .map(this::ensureCustomerForUser)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản hoặc khách hàng với ID cung cấp")));
    }

    private void ensureWalletExists(Customer customer) {
        // Với @MapsId, ID của Wallet trùng với ID của Customer
        walletRepository.findById(customer.getId())
                .orElseGet(() -> walletRepository.saveAndFlush(Wallet.builder()
                        .customer(customer) // Hibernate tự lấy ID từ customer gán vào Wallet
                        .balance(BigDecimal.ZERO)
                        .build()));
    }

    public Optional<Customer> getCustomerById(UUID id) {
        // Tìm kiếm khách hàng theo ID vạn năng đã đồng bộ
        return customerRepository.findById(id);
    }
    
    // =========================================================================
    // CÁC HÀM XỬ LÝ RIÊNG CHO ĐỐI TÁC B2B (ENTERPRISE)
    // =========================================================================

    @Transactional(readOnly = true)
    public List<Customer> getAllEnterprisePartners() {
        // Lấy danh sách chỉ riêng khách hàng Doanh nghiệp
        return customerRepository.findByCustomerType(CustomerType.ENTERPRISE);
    }

    @Transactional
    public Customer createEnterprisePartner(Customer dto) {
        // 1. Validate dữ liệu B2B bắt buộc
        if (dto.getCompanyName() == null || dto.getCompanyName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên doanh nghiệp không được để trống!");
        }
        if (dto.getTaxCode() == null || dto.getTaxCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Mã số thuế không được để trống!");
        }
        if (dto.getBillingAddress() == null || dto.getBillingAddress().trim().isEmpty()) {
            throw new IllegalArgumentException("Địa chỉ xuất hóa đơn không được để trống!");
        }

        // 2. Kiểm tra trùng lặp Mã số thuế
        Optional<Customer> existingTaxCode = customerRepository.findByTaxCode(dto.getTaxCode().trim());
        if (existingTaxCode.isPresent()) {
            throw new IllegalArgumentException("Mã số thuế này đã được đăng ký cho doanh nghiệp: " + existingTaxCode.get().getCompanyName());
        }

        // 3. Gán mặc định các giá trị của một đối tác B2B
        Customer enterpriseCustomer = Customer.builder()
                .fullName(dto.getFullName()) // Tên người đại diện
                .phoneNumber(dto.getPhoneNumber())
                .email(dto.getEmail())
                .customerType(CustomerType.ENTERPRISE) // Bắt buộc gán type là ENTERPRISE
                .companyName(dto.getCompanyName().trim())
                .taxCode(dto.getTaxCode().trim())
                .billingAddress(dto.getBillingAddress())
                .totalSpent(BigDecimal.ZERO)
                .creditLimit(new BigDecimal("50000000"))
                .build();

        return customerRepository.save(enterpriseCustomer);
    }
    
    @Transactional(readOnly = true)
    public Customer searchCustomerForPOS(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new IllegalArgumentException("Từ khóa tìm kiếm không được để trống!");
        }
        
        String cleanKeyword = keyword.trim();
        
        // 1. Tìm theo Số điện thoại
        Optional<Customer> byPhone = customerRepository.findByPhoneNumber(cleanKeyword);
        if (byPhone.isPresent()) {
            return byPhone.get();
        }
        
        // 2. Nếu không thấy, tìm tiếp theo Mã số thuế (B2B)
        Optional<Customer> byTaxCode = customerRepository.findByTaxCode(cleanKeyword);
        if (byTaxCode.isPresent()) {
            return byTaxCode.get();
        }
        
        throw new IllegalArgumentException("Không tìm thấy hồ sơ khách hàng nào khớp với: " + cleanKeyword);
    }


    @Transactional
    public Customer updateEnterprisePartner(UUID id, Customer updateData) {
        Customer existing = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hồ sơ đối tác!"));

        if (existing.getCustomerType() != CustomerType.ENTERPRISE) {
            throw new IllegalArgumentException("Đây không phải là hồ sơ Doanh nghiệp B2B!");
        }

        // Kiểm tra mã số thuế nếu có sự thay đổi
        if (!existing.getTaxCode().equals(updateData.getTaxCode())) {
            if (customerRepository.findByTaxCode(updateData.getTaxCode()).isPresent()) {
                throw new IllegalArgumentException("Mã số thuế mới bị trùng lặp với doanh nghiệp khác!");
            }
            existing.setTaxCode(updateData.getTaxCode());
        }

        if (updateData.getBillingAddress() == null || updateData.getBillingAddress().trim().isEmpty()) {
            throw new IllegalArgumentException("Địa chỉ xuất hóa đơn không được để trống!");
        }

        existing.setCompanyName(updateData.getCompanyName());
        existing.setBillingAddress(updateData.getBillingAddress());
        existing.setFullName(updateData.getFullName());
        existing.setPhoneNumber(updateData.getPhoneNumber());
        existing.setEmail(updateData.getEmail());

        return customerRepository.save(existing);
    }
}
