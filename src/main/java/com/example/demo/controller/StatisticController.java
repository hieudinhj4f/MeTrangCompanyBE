package com.example.demo.controller;

import com.example.demo.dto.response.RevenueChartDto;
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
import java.util.List;

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
}