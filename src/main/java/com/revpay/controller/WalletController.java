package com.revpay.controller;

import com.revpay.model.dto.*;
import com.revpay.model.entity.*;
import com.revpay.repository.UserRepository;
import com.revpay.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wallet")

public class WalletController {

    @Autowired private WalletService walletService;
    @Autowired private UserRepository userRepository;

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }

    // --- 1. CORE OPERATIONS ---

    @GetMapping("/balance")
    public ResponseEntity<BigDecimal> getBalance() {
        return ResponseEntity.ok(walletService.getBalance(getAuthenticatedUser().getUserId()));
    }

    @PostMapping("/send")
    public ResponseEntity<Transaction> transfer(@RequestBody TransactionRequest request) {
        return ResponseEntity.ok(walletService.sendMoney(getAuthenticatedUser().getUserId(), request));
    }

    // FIXED: Now accepts JSON body instead of Params for better Postman testing
    @PostMapping("/add-funds")
    public ResponseEntity<Transaction> addFunds(@RequestBody Map<String, Object> request) {
        BigDecimal amount = new BigDecimal(request.get("amount").toString());
        String description = request.getOrDefault("description", "Wallet Deposit").toString();
        return ResponseEntity.ok(walletService.addFunds(getAuthenticatedUser().getUserId(), amount, description));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<Transaction> withdraw(@RequestBody Map<String, Object> request) {
        BigDecimal amount = new BigDecimal(request.get("amount").toString());
        return ResponseEntity.ok(walletService.withdrawFunds(getAuthenticatedUser().getUserId(), amount));
    }

    // --- 2. INVOICE & REQUESTS ---

    @PostMapping("/pay-invoice")
    public ResponseEntity<Transaction> payInvoice(@RequestBody InvoicePaymentRequest request) {
        return ResponseEntity.ok(walletService.payInvoice(
                getAuthenticatedUser().getUserId(),
                request.getInvoiceId(),
                request.getTransactionPin()
        ));
    }

    @PostMapping("/request")
    public ResponseEntity<Transaction> requestMoney(@RequestBody Map<String, Object> request) {
        String targetEmail = request.get("targetEmail").toString();
        BigDecimal amount = new BigDecimal(request.get("amount").toString());
        return ResponseEntity.ok(walletService.requestMoney(getAuthenticatedUser().getUserId(), targetEmail, amount));
    }

    @PostMapping("/request/accept/{txnId}")
    public ResponseEntity<Transaction> acceptRequest(@PathVariable Long txnId, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(walletService.acceptRequest(txnId, body.get("pin")));
    }

    // --- 3. HISTORY & ANALYTICS ---

    @GetMapping("/transactions")
    public ResponseEntity<List<Transaction>> getHistory(@RequestParam(required = false) String type) {
        User user = getAuthenticatedUser();
        if (type != null && !type.isEmpty()) {
            return ResponseEntity.ok(walletService.getMyHistory(user, type));
        }
        return ResponseEntity.ok(walletService.getTransactionHistory(user.getUserId()));
    }

    @GetMapping("/analytics")
    public ResponseEntity<WalletAnalyticsDTO> getAnalytics() {
        return ResponseEntity.ok(walletService.getSpendingAnalytics(getAuthenticatedUser()));
    }

    // --- 4. CARD MANAGEMENT ---

    @GetMapping("/cards")
    public ResponseEntity<List<PaymentMethod>> getMyCards() {
        return ResponseEntity.ok(walletService.getCards(getAuthenticatedUser().getUserId()));
    }

    @PostMapping("/cards")
    public ResponseEntity<PaymentMethod> addCard(@RequestBody PaymentMethod card) {
        return ResponseEntity.ok(walletService.addCard(getAuthenticatedUser().getUserId(), card));
    }
}