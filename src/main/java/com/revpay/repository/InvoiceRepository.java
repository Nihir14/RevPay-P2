package com.revpay.repository;

import com.revpay.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    // Find invoices by business profile ID
    List<Invoice> findByBusinessProfile_ProfileId(Long profileId);

    // Filter by status
    List<Invoice> findByStatus(Invoice.InvoiceStatus status);

} // This closing brace MUST be at the end

