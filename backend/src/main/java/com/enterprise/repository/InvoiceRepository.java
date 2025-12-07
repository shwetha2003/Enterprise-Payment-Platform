package com.enterprise.repository;

import com.enterprise.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByCustomerId(Long customerId);
    
    List<Invoice> findByStatus(String status);
    
    @Query("SELECT i FROM Invoice i WHERE i.dueDate < CURRENT_DATE AND i.status = 'PENDING'")
    List<Invoice> findOverdueInvoices();
    
    @Query("SELECT SUM(i.amount) FROM Invoice i WHERE i.status = 'PENDING'")
    Double getTotalOutstanding();
    
    @Query(value = """
        SELECT 
            status,
            COUNT(*) as count,
            SUM(amount) as total_amount
        FROM invoices
        GROUP BY status
    """, nativeQuery = true)
    List<Object[]> getInvoiceSummary();
}
