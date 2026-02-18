package com.revpay.controller;

import com.revpay.model.Invoice;
import com.revpay.service.InvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/business")
public class InvoiceController {

    @Autowired
    private InvoiceService invoiceService;

    @PostMapping("/invoices")
    public ResponseEntity<Invoice> create(@RequestBody Invoice invoice) {
        return ResponseEntity.ok(invoiceService.createInvoice(invoice));
    }

    @GetMapping("/{businessId}/invoices")
    public List<Invoice> getInvoices(@PathVariable Long businessId) {
        return invoiceService.getAllInvoicesByBusiness(businessId);
    }

    @PatchMapping("/invoices/{id}/pay")
    public ResponseEntity<String> markPaid(@PathVariable Long id) {
        invoiceService.markAsPaid(id);
        return ResponseEntity.ok("Invoice updated to PAID status");
    }
}