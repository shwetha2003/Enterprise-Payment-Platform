package com.enterprise.controller;

import com.enterprise.model.Invoice;
import com.enterprise.repository.InvoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/invoices")
@CrossOrigin(origins = "*")
public class InvoiceController {
    
    @Autowired
    private InvoiceRepository invoiceRepository;
    
    @GetMapping
    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAll();
    }
    
    @GetMapping("/{id}")
    public Invoice getInvoice(@PathVariable Long id) {
        return invoiceRepository.findById(id).orElse(null);
    }
    
    @PostMapping
    public Invoice createInvoice(@RequestBody Invoice invoice) {
        return invoiceRepository.save(invoice);
    }
    
    @GetMapping("/customer/{customerId}")
    public List<Invoice> getCustomerInvoices(@PathVariable Long customerId) {
        return invoiceRepository.findByCustomerId(customerId);
    }
    
    @GetMapping("/overdue")
    public List<Invoice> getOverdueInvoices() {
        return invoiceRepository.findOverdueInvoices();
    }
    
    @GetMapping("/status/{status}")
    public List<Invoice> getInvoicesByStatus(@PathVariable String status) {
        return invoiceRepository.findByStatus(status);
    }
}
