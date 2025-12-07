package com.enterprise.model;

import lombok.Data;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "invoices")
@Data
public class Invoice {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String invoiceNumber;
    
    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
    
    @Column(nullable = false)
    private LocalDate invoiceDate;
    
    @Column(nullable = false)
    private LocalDate dueDate;
    
    @Column(precision = 15, scale = 2)
    private BigDecimal subtotal;
    
    @Column(precision = 15, scale = 2)
    private BigDecimal tax;
    
    @Column(precision = 15, scale = 2)
    private BigDecimal total;
    
    @Enumerated(EnumType.STRING)
    private InvoiceStatus status;
    
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL)
    private List<InvoiceLineItem> lineItems;
    
    @OneToMany(mappedBy = "invoice")
    private List<Payment> payments;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    public enum InvoiceStatus {
        DRAFT, ISSUED, PARTIALLY_PAID, PAID, OVERDUE, VOID
    }
}
