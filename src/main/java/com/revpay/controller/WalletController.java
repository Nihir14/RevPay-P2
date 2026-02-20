package com.revpay.controller;

import com.revpay.model.dto.InvoicePaymentRequest;
import com.revpay.model.dto.TransactionRequest;
import com.revpay.model.dto.WalletAnalyticsDTO;
import com.revpay.model.entity.Transaction;
import com.revpay.model.entity.User;
import com.revpay.repository.UserRepository;
import com.revpay.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/wallet")
@CrossOrigin(origins = "*")
public class WalletController {

    @Autowired private WalletService walletService;
    @Autowired private UserRepository userRepository;

    /**
     * Helper to get logged-in user from Security Context.
     * Centralizing this makes the controller cleaner.
     */
    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }

    // --- 1. WALLET BALANCE ---
    @GetMapping("/balance")
    public ResponseEntity<BigDecimal> getBalance() {
        return ResponseEntity.ok(walletService.getBalance(getAuthenticatedUser().getUserId()));
    }

    // --- 2. MONEY TRANSFERS (P2P) ---
    @PostMapping("/send")
    public ResponseEntity<Transaction> transfer(@RequestBody TransactionRequest request) {
        User sender = getAuthenticatedUser();
        return ResponseEntity.ok(walletService.sendMoney(sender.getUserId(), request));
    }

    // --- 3. INVOICE PAYMENTS ---
    @PostMapping("/pay-invoice")
    public ResponseEntity<Transaction> payInvoice(@RequestBody InvoicePaymentRequest request) {
        User user = getAuthenticatedUser();
        return ResponseEntity.ok(walletService.payInvoice(
                user.getUserId(),
                request.getInvoiceId(),
                request.getTransactionPin()
        ));
    }

    // --- 4. TRANSACTION HISTORY (UPDATED) ---
    @GetMapping("/transactions")
    public ResponseEntity<List<Transaction>> getHistory(@RequestParam(required = false) String type) {
        User user = getAuthenticatedUser();

        // If a specific type is requested (e.g., SEND, ADD_FUNDS), use filtered history
        if (type != null && !type.isEmpty()) {
            return ResponseEntity.ok(walletService.getMyHistory(user, type));
        }

        // Otherwise, return the full standard history for the user
        return ResponseEntity.ok(walletService.getTransactionHistory(user.getUserId()));
    }

    // --- 5. WALLET ANALYTICS ---
    @GetMapping("/analytics")
    public ResponseEntity<WalletAnalyticsDTO> getAnalytics() {
        return ResponseEntity.ok(walletService.getSpendingAnalytics(getAuthenticatedUser()));
    }

    // --- 6. ADD & WITHDRAW FUNDS ---
    @PostMapping("/add-funds")
    public ResponseEntity<Transaction> addFunds(
            @RequestParam BigDecimal amount,
            @RequestParam(required = false, defaultValue = "Wallet Deposit") String description) {
        return ResponseEntity.ok(walletService.addFunds(getAuthenticatedUser().getUserId(), amount, description));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<Transaction> withdraw(@RequestParam BigDecimal amount) {
        return ResponseEntity.ok(walletService.withdrawFunds(getAuthenticatedUser().getUserId(), amount));
    }
}