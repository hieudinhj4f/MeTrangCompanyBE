package com.example.demo.service;

import com.example.demo.entity.Supplier;
import com.example.demo.repository.SupplierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class SupplierService {

    @Autowired
    private SupplierRepository supplierRepository;

    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findAll();
    }

    public Optional<Supplier> getSupplierById(Integer id) {
        return supplierRepository.findById(id);
    }

    public Supplier saveSupplier(Supplier supplier) {
        if (supplier.getName() == null || supplier.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên nhà cung cấp không được để trống!");
        }
        if (supplier.getTaxCode() == null || supplier.getTaxCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Mã số thuế nhà cung cấp không được để trống!");
        }
        if (supplier.getAddress() == null || supplier.getAddress().trim().isEmpty()) {
            throw new IllegalArgumentException("Địa chỉ nhà cung cấp không được để trống!");
        }

        // Check unique tax code
        Optional<Supplier> existing = supplierRepository.findByTaxCode(supplier.getTaxCode().trim());
        if (existing.isPresent() && !existing.get().getId().equals(supplier.getId())) {
            throw new IllegalArgumentException("Mã số thuế này đã được đăng ký cho nhà cung cấp: " + existing.get().getName());
        }

        if (supplier.getIsActive() == null) {
            supplier.setIsActive(true);
        }
        return supplierRepository.save(supplier);
    }

    public void deleteSupplier(Integer id) {
        supplierRepository.deleteById(id);
    }
}