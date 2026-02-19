package com.revpay.repository;

import com.revpay.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByBusinessProfile_ProfileId(Long businessId); // Find invoices by business
    List<Invoice> findByStatus(Invoice.InvoiceStatus status); // Filter by status
}

    // ✅ FIXED: Changed 'Id' to 'ProfileId' to match your Entity field name
    List<Invoice> findByBusinessProfile_ProfileId(Long profileId);

    // Keep this one as is
    List<Invoice> findByStatus(Invoice.InvoiceStatus status);
}
