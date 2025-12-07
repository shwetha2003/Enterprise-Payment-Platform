package com.enterprise.model;

import lombok.Data;
import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "customers")
@Data
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String companyName;
    
    private String email;
    
    private String status;
    
    @OneToMany(mappedBy = "customer")
    private List<Invoice> invoices;
}
