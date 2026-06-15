package com.example.demo.controller;

import com.example.demo.service.StatisticService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.demo.dto.response.FinanceDashboardResponse;
import com.example.demo.dto.response.InventoryDashboardResponse;
import com.example.demo.dto.response.OperationDashboardResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.io.ByteArrayInputStream;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticController {

    private final StatisticService statisticService;

    @GetMapping("/finance")
    public ResponseEntity<FinanceDashboardResponse> getFinanceStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);
        return ResponseEntity.ok(statisticService.getFinanceDashboard(start, end));
    }

    @GetMapping("/operations")
    public ResponseEntity<OperationDashboardResponse> getOperationsStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);
        return ResponseEntity.ok(statisticService.getOperationDashboard(start, end));
    }
    @GetMapping("/inventory")
    public ResponseEntity<InventoryDashboardResponse> getInventoryStatistics() {
        return ResponseEntity.ok(statisticService.getInventoryDashboard());
    }

    @GetMapping("/export/finance")
    public ResponseEntity<InputStreamResource> exportFinanceExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);
        
        ByteArrayInputStream in = statisticService.exportFinanceReportToExcel(start, end);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=Bao_Cao_Doanh_Thu_" + startDate + "_to_" + endDate + ".xlsx");
        
        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(in));
    }
}