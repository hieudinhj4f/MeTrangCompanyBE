package com.example.demo.controller;

import com.example.demo.entity.Supplier;
import com.example.demo.service.SupplierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class SupplierController {

    @Autowired
    private SupplierService supplierService;

    @GetMapping
    public ResponseEntity<List<Supplier>> getAllSuppliers() {
        return ResponseEntity.ok(supplierService.getAllSuppliers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Supplier> getSupplierById(@PathVariable Integer id) {
        return supplierService.getSupplierById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Supplier> createSupplier(@RequestBody Supplier supplier) {
        return ResponseEntity.ok(supplierService.saveSupplier(supplier));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Supplier> updateSupplier(@PathVariable Integer id, @RequestBody Supplier details) {
        return supplierService.getSupplierById(id).map(supplier -> {
            supplier.setName(details.getName());
            supplier.setContactName(details.getContactName());
            supplier.setPhoneNumber(details.getPhoneNumber());
            supplier.setEmail(details.getEmail());
            supplier.setAddress(details.getAddress());
            supplier.setTaxCode(details.getTaxCode());
            supplier.setIsActive(details.getIsActive());
            return ResponseEntity.ok(supplierService.saveSupplier(supplier));
        }).orElse(ResponseEntity.notFound().build());
    }
}