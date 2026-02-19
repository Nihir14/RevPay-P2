package com.revpay.service;

import com.revpay.model.entity.BusinessProfile;
import com.revpay.model.entity.Invoice;
import com.revpay.repository.BusinessProfileRepository;
import com.revpay.repository.InvoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class InvoiceService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private BusinessProfileRepository businessProfileRepository;

    public Invoice createInvoice(Long profileId, Invoice invoice) {
        BusinessProfile business = businessProfileRepository.findById(profileId)
                .orElseThrow(() -> new RuntimeException("Business not found"));

        invoice.setBusinessProfile(business);

        if (invoice.getStatus() == null) {
            invoice.setStatus(Invoice.InvoiceStatus.DRAFT);
        }
        return invoiceRepository.save(invoice);
    }

    // Consolidated the duplicate methods into one
    public List<Invoice> getAllInvoicesByBusiness(Long profileId) {
        return invoiceRepository.findByBusinessProfile_ProfileId(profileId);
    }

    public void markAsPaid(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        invoice.setStatus(Invoice.InvoiceStatus.PAID);
        invoiceRepository.save(invoice);
    }
}