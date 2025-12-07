package com.enterprise.controller;

import com.enterprise.model.*;
import com.enterprise.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Financial analytics and business intelligence")
public class AnalyticsController {
    
    private final AnalyticsService analyticsService;
    
    @GetMapping("/mrr")
    @PreAuthorize("hasRole('ANALYST') or hasRole('ADMIN')")
    @Operation(summary = "Get Monthly Recurring Revenue")
    public ResponseEntity<MRRAnalytics> getMRR(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(analyticsService.calculateMRR(startDate, endDate));
    }
    
    @GetMapping("/churn")
    @PreAuthorize("hasRole('ANALYST') or hasRole('ADMIN')")
    @Operation(summary = "Calculate churn rate")
    public ResponseEntity<ChurnAnalysis> getChurnRate(
            @RequestParam String period) {
        return ResponseEntity.ok(analyticsService.calculateChurnRate(period));
    }
    
    @GetMapping("/ar-aging")
    @PreAuthorize("hasRole('ANALYST') or hasRole('ADMIN')")
    @Operation(summary = "Get AR Aging Report")
    public ResponseEntity<ARAgingReport> getARAgingReport() {
        return ResponseEntity.ok(analyticsService.generateARAgingReport());
    }
    
    @GetMapping("/payment-trends")
    @PreAuthorize("hasRole('ANALYST') or hasRole('ADMIN')")
    @Operation(summary = "Analyze payment trends")
    public ResponseEntity<List<PaymentTrend>> getPaymentTrends(
            @RequestParam(required = false) String customerId) {
        return ResponseEntity.ok(analyticsService.analyzePaymentTrends(customerId));
    }
    
    @GetMapping("/key-metrics")
    @PreAuthorize("hasRole('ANALYST') or hasRole('ADMIN')")
    @Operation(summary = "Get all key business metrics")
    public ResponseEntity<Map<String, Object>> getKeyMetrics() {
        return ResponseEntity.ok(analyticsService.getKeyBusinessMetrics());
    }
    
    @GetMapping("/forecast/cash-flow")
    @PreAuthorize("hasRole('ANALYST') or hasRole('ADMIN')")
    @Operation(summary = "Forecast cash flow")
    public ResponseEntity<CashFlowForecast> getCashFlowForecast(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(analyticsService.forecastCashFlow(days));
    }
}
