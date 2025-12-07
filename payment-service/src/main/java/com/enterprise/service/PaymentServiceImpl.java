package com.enterprise.service;

import com.enterprise.model.*;
import com.enterprise.repository.InvoiceRepository;
import com.enterprise.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {
    
    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final EmailService emailService;
    private final PaymentAnalyticsService analyticsService;
    
    @Override
    @Transactional
    public PaymentResult processPayment(PaymentRequest request) {
        validatePaymentRequest(request);
        
        Invoice invoice = invoiceRepository.findById(request.getInvoiceId())
            .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
        
        // Check if invoice is already paid
        if (invoice.getStatus() == Invoice.InvoiceStatus.PAID) {
            throw new PaymentException("Invoice is already paid");
        }
        
        // Create payment record
        Payment payment = Payment.builder()
            .paymentReference(UUID.randomUUID().toString())
            .invoice(invoice)
            .customer(invoice.getCustomer())
            .amount(request.getAmount())
            .paymentMethod(request.getPaymentMethod())
            .paymentDate(LocalDateTime.now())
            .status(Payment.PaymentStatus.PENDING)
            .build();
        
        // Simulate payment processing (in real app, integrate with Stripe/Braintree)
        boolean paymentSuccess = simulatePaymentProcessing(payment);
        
        if (paymentSuccess) {
            payment.setStatus(Payment.PaymentStatus.COMPLETED);
            updateInvoiceStatus(invoice, payment.getAmount());
            
            // Send confirmation
            emailService.sendPaymentConfirmation(invoice.getCustomer(), payment);
            
            // Update analytics
            analyticsService.recordPayment(payment);
            
            log.info("Payment processed successfully: {}", payment.getPaymentReference());
        } else {
            payment.setStatus(Payment.PaymentStatus.FAILED);
            payment.setFailureReason("Payment processor declined");
            
            // Trigger dunning process
            handleFailedPayment(invoice, payment);
        }
        
        paymentRepository.save(payment);
        
        return PaymentResult.builder()
            .success(paymentSuccess)
            .paymentId(payment.getId())
            .reference(payment.getPaymentReference())
            .message(paymentSuccess ? "Payment successful" : "Payment failed")
            .build();
    }
    
    @Override
    public List<Invoice> getOutstandingInvoices(String customerId) {
        return invoiceRepository.findOutstandingInvoicesByCustomer(customerId);
    }
    
    @Override
    public PaymentTrends analyzePaymentPatterns(String customerId) {
        List<Payment> customerPayments = paymentRepository.findByCustomerId(customerId);
        
        PaymentTrends trends = new PaymentTrends();
        trends.setCustomerId(customerId);
        trends.setTotalPayments(customerPayments.size());
        
        // Calculate average days to pay
        double avgDaysToPay = customerPayments.stream()
            .filter(p -> p.getStatus() == Payment.PaymentStatus.COMPLETED)
            .mapToLong(p -> calculateDaysBetweenInvoiceAndPayment(p))
            .average()
            .orElse(0);
        trends.setAverageDaysToPay(avgDaysToPay);
        
        // Calculate on-time payment rate
        long onTimePayments = customerPayments.stream()
            .filter(p -> p.getStatus() == Payment.PaymentStatus.COMPLETED)
            .filter(this::isPaymentOnTime)
            .count();
        
        trends.setOnTimePaymentRate(customerPayments.isEmpty() ? 0 : 
            (double) onTimePayments / customerPayments.size() * 100);
        
        return trends;
    }
    
    @Override
    public Receipt generateReceipt(String paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        
        if (payment.getStatus() != Payment.PaymentStatus.COMPLETED) {
            throw new PaymentException("Cannot generate receipt for incomplete payment");
        }
        
        return Receipt.builder()
            .receiptNumber("RCPT-" + payment.getPaymentReference())
            .paymentDate(payment.getPaymentDate())
            .customerName(payment.getCustomer().getCompanyName())
            .invoiceNumber(payment.getInvoice().getInvoiceNumber())
            .amount(payment.getAmount())
            .paymentMethod(payment.getPaymentMethod())
            .build();
    }
    
    private boolean simulatePaymentProcessing(Payment payment) {
        // Simulate payment gateway response
        // In production, integrate with Stripe/Braintree/Adyen
        return Math.random() > 0.1; // 90% success rate for simulation
    }
    
    private void updateInvoiceStatus(Invoice invoice, BigDecimal paymentAmount) {
        BigDecimal remaining = invoice.getTotal().subtract(getTotalPaid(invoice));
        BigDecimal newRemaining = remaining.subtract(paymentAmount);
        
        if (newRemaining.compareTo(BigDecimal.ZERO) <= 0) {
            invoice.setStatus(Invoice.InvoiceStatus.PAID);
        } else if (newRemaining.compareTo(invoice.getTotal()) < 0) {
            invoice.setStatus(Invoice.InvoiceStatus.PARTIALLY_PAID);
        }
        
        invoiceRepository.save(invoice);
    }
    
    private BigDecimal getTotalPaid(Invoice invoice) {
        return invoice.getPayments().stream()
            .filter(p -> p.getStatus() == Payment.PaymentStatus.COMPLETED)
            .map(Payment::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    private void validatePaymentRequest(PaymentRequest request) {
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Payment amount must be positive");
        }
        
        if (request.getPaymentMethod() == null) {
            throw new ValidationException("Payment method is required");
        }
    }
}
