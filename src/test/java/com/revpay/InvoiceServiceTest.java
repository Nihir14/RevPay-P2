package com.revpay;

import com.revpay.model.entity.BusinessProfile;
import com.revpay.model.entity.Invoice;
import com.revpay.repository.BusinessProfileRepository;
import com.revpay.repository.InvoiceRepository;
import com.revpay.service.InvoiceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class InvoiceServiceTest {

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private BusinessProfileRepository businessProfileRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    private BusinessProfile testProfile;

    @BeforeEach
    void setUp() {
        testProfile = new BusinessProfile();
        // Ensure BusinessProfile also has getProfileId() or getId() defined correctly
        testProfile = businessProfileRepository.save(testProfile);
    }

    @Test
    void testCreateInvoice_SetsDraftStatus() {
        Invoice invoice = new Invoice();
        // Add dummy data to avoid null pointer exceptions if your entity has nullable=false
        invoice.setCustomerName("Test Customer");
        invoice.setCustomerEmail("test@example.com");
        invoice.setTotalAmount(new java.math.BigDecimal("100.00"));

        Invoice savedInvoice = invoiceService.createInvoice(testProfile.getProfileId(), invoice);

        // CHANGED: getInvoiceId() -> getId()
        assertNotNull(savedInvoice.getId());
        assertEquals(Invoice.InvoiceStatus.DRAFT, savedInvoice.getStatus());
        assertEquals(testProfile.getProfileId(), savedInvoice.getBusinessProfile().getProfileId());
    }

    @Test
    void testGetAllInvoicesByBusiness() {
        Invoice invoice = new Invoice();
        invoice.setCustomerName("Test Customer");
        invoice.setCustomerEmail("test@example.com");
        invoice.setTotalAmount(new java.math.BigDecimal("100.00"));

        invoiceService.createInvoice(testProfile.getProfileId(), invoice);

        List<Invoice> result = invoiceService.getAllInvoicesByBusiness(testProfile.getProfileId());

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    void testMarkAsPaid_UpdatesStatus() {
        Invoice invoice = new Invoice();
        invoice.setCustomerName("Test Customer");
        invoice.setCustomerEmail("test@example.com");
        invoice.setTotalAmount(new java.math.BigDecimal("100.00"));

        Invoice savedInvoice = invoiceService.createInvoice(testProfile.getProfileId(), invoice);

        // CHANGED: getInvoiceId() -> getId()
        invoiceService.markAsPaid(savedInvoice.getId());

        // CHANGED: getInvoiceId() -> getId()
        Invoice updatedInvoice = invoiceRepository.findById(savedInvoice.getId()).get();
        assertEquals(Invoice.InvoiceStatus.PAID, updatedInvoice.getStatus());
    }

    @Test
    void testCreateInvoice_ThrowsExceptionWhenBusinessNotFound() {
        assertThrows(RuntimeException.class, () -> {
            invoiceService.createInvoice(999L, new Invoice());
        });
    }
}