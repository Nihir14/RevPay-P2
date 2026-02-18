package com.revpay.service;

import com.revpay.model.Invoice;
import com.revpay.repository.InvoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class InvoiceService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    public Invoice createInvoice(Invoice invoice) {
        // Set default status to DRAFT
        if (invoice.getStatus() == null) {
            invoice.setStatus(Invoice.InvoiceStatus.DRAFT);
        }
        return invoiceRepository.save(invoice);
    }

    public List<Invoice> getAllInvoicesByBusiness(Long businessId) {
        return invoiceRepository.findByBusinessProfileId(businessId);
    }

    public void markAsPaid(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));
        invoice.setStatus(Invoice.InvoiceStatus.PAID); // Requirement: Mark invoices as paid manually
        invoiceRepository.save(invoice);
    }
}


