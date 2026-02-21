package com.revpay.controller;

import com.revpay.model.entity.Invoice;
import com.revpay.model.entity.BusinessProfile;
import com.revpay.service.InvoiceService;
import com.revpay.repository.BusinessProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')") // Role-based access control
public class AdminController {

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private BusinessProfileRepository businessProfileRepository;

    // Requirement: View all invoices across the platform
    @GetMapping("/invoices")
    public ResponseEntity<List<Invoice>> getAllSystemInvoices() {
        return ResponseEntity.ok(invoiceService.getAllInvoices());
    }

    // Requirement: View all business profiles for management
    @GetMapping("/businesses")
    public ResponseEntity<List<BusinessProfile>> getAllBusinessProfiles() {
        return ResponseEntity.ok(businessProfileRepository.findAll());
    }

    // Requirement: Business verification for account approval (simulated)
    @PostMapping("/businesses/{id}/verify")
    public ResponseEntity<String> verifyBusiness(@PathVariable Long id) {
        BusinessProfile business = businessProfileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Business not found"));

        // Logic to update verification status would go here
        return ResponseEntity.ok("Business account verified successfully.");
    }
}