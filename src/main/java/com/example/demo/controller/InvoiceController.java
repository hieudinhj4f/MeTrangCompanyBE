package com.example.demo.controller;

import com.example.demo.entity.Invoice;
import com.example.demo.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceRepository invoiceRepository;

    @GetMapping
    public ResponseEntity<List<Invoice>> getAllInvoices() {
        return ResponseEntity.ok(invoiceRepository.findAll());
    }

    @GetMapping("/pending")
    public ResponseEntity<List<Invoice>> getPendingInvoices() {
        return ResponseEntity.ok(invoiceRepository.findByIsIssued(false));
    }

    @PostMapping("/{id}/issue")
    public ResponseEntity<?> issueInvoice(@PathVariable UUID id, @RequestBody Map<String, String> payload) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu xuất hóa đơn!"));

        String eInvoiceNumber = payload.get("eInvoiceNumber");
        if (eInvoiceNumber == null || eInvoiceNumber.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("reason", "Vui lòng cung cấp số hóa đơn điện tử"));
        }

        invoice.setEInvoiceNumber(eInvoiceNumber);
        invoice.setIsIssued(true);
        invoiceRepository.save(invoice);

        return ResponseEntity.ok(Map.of("message", "Đã đánh dấu xuất hóa đơn thành công", "invoice", invoice));
    }
}
