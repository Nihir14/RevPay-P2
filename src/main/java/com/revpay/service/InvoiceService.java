package com.revpay.service;

import com.revpay.model.entity.BusinessProfile;
import com.revpay.model.entity.Invoice;
import com.revpay.repository.BusinessProfileRepository;
import com.revpay.repository.InvoiceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Slf4j
@Service
public class InvoiceService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private BusinessProfileRepository businessProfileRepository;

    public Invoice createInvoice(Long profileId, Invoice invoice) {
        log.info("Attempting to create invoice for business profile ID: {}", profileId);

        BusinessProfile business = businessProfileRepository.findById(profileId)
                .orElseThrow(() -> {
                    log.error("Failed to create invoice: Business profile not found for ID {}", profileId);
                    return new RuntimeException("Business not found");
                });

        invoice.setBusinessProfile(business);

        if (invoice.getStatus() == null) {
            log.debug("Invoice status was null, defaulting to DRAFT");
            invoice.setStatus(Invoice.InvoiceStatus.DRAFT);
        }

        Invoice savedInvoice = invoiceRepository.save(invoice);
        log.info("Successfully created invoice with ID: {} for business: {}", savedInvoice.getId(), business.getProfileId());
        return savedInvoice;
    }

    public List<Invoice> getAllInvoicesByBusiness(Long profileId) {
        log.info("Fetching all invoices for business profile ID: {}", profileId);
        List<Invoice> invoices = invoiceRepository.findByBusinessProfile_ProfileId(profileId);
        log.debug("Found {} invoices for business profile ID: {}", invoices.size(), profileId);
        return invoices;
    }

    public void markAsPaid(Long invoiceId) {
        log.info("Attempting to mark invoice ID: {} as PAID", invoiceId);

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> {
                    log.error("Update failed: Invoice not found with ID {}", invoiceId);
                    return new RuntimeException("Invoice not found");
                });

        invoice.setStatus(Invoice.InvoiceStatus.PAID);
        invoiceRepository.save(invoice);
        log.info("Successfully marked invoice ID: {} as PAID", invoiceId);
    }
}