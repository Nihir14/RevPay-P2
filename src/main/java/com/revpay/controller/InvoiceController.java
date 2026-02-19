package com.revpay.controller;

import com.revpay.model.Invoice;
import com.revpay.service.InvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/business")
@CrossOrigin(origins = "*")
public class InvoiceController {

    @Autowired
    private InvoiceService invoiceService;

    // POST /api/business/{profileId}/invoices
    @PostMapping("/{profileId}/invoices")
    public ResponseEntity<Invoice> create(@PathVariable Long profileId, @RequestBody Invoice invoice) {
        return ResponseEntity.ok(invoiceService.createInvoice(profileId, invoice));
    }

    // GET /api/business/{profileId}/invoices
    @GetMapping("/{profileId}/invoices")
    public List<Invoice> getInvoices(@PathVariable Long profileId) {
        return invoiceService.getAllInvoicesByBusiness(profileId);
    }

    // PATCH /api/business/invoices/{id}/pay
    @PatchMapping("/invoices/{id}/pay")
    public ResponseEntity<String> markPaid(@PathVariable Long id) {
        invoiceService.markAsPaid(id);
        return ResponseEntity.ok("Invoice updated to PAID status");
    }
}