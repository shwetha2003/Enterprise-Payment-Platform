package com.enterprise.controller;

import com.enterprise.repository.InvoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "*")
public class AnalyticsController {
    
    @Autowired
    private InvoiceRepository invoiceRepository;
    
    @GetMapping("/dashboard")
    public Map<String, Object> getDashboardMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        Double totalOutstanding = invoiceRepository.getTotalOutstanding();
        metrics.put("totalOutstanding", totalOutstanding != null ? totalOutstanding : 0);
        
        List<Invoice> overdue = invoiceRepository.findOverdueInvoices();
        metrics.put("overdueCount", overdue.size());
        
        Double overdueAmount = overdue.stream()
            .mapToDouble(i -> i.getAmount().doubleValue())
            .sum();
        metrics.put("overdueAmount", overdueAmount);
        
        List<Object[]> summary = invoiceRepository.getInvoiceSummary();
        Map<String, Object> statusSummary = new HashMap<>();
        for (Object[] row : summary) {
            statusSummary.put(row[0].toString(), Map.of(
                "count", row[1],
                "amount", row[2]
            ));
        }
        metrics.put("statusSummary", statusSummary);
        
        // Mock revenue data
        List<Map<String, Object>> revenueData = new ArrayList<>();
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun"};
        Random random = new Random();
        
        for (int i = 0; i < months.length; i++) {
            Map<String, Object> monthData = new HashMap<>();
            monthData.put("month", months[i]);
            monthData.put("revenue", 5000 + random.nextInt(10000));
            monthData.put("forecast", 5500 + random.nextInt(10000));
            revenueData.add(monthData);
        }
        metrics.put("revenueData", revenueData);
        
        return metrics;
    }
    
    @GetMapping("/aging-report")
    public Map<String, Object> getAgingReport() {
        Map<String, Object> report = new HashMap<>();
        
        // Mock aging data
        report.put("current", 15000.00);
        report.put("days_1_30", 7500.00);
        report.put("days_31_60", 3500.00);
        report.put("days_61_90", 1800.00);
        report.put("over_90", 1200.00);
        
        return report;
    }
}
