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
public class WalletController {

    @Autowired private WalletService walletService;
    @Autowired private UserRepository userRepository;

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }

    @GetMapping("/balance")
    public ResponseEntity<BigDecimal> getBalance() {
        return ResponseEntity.ok(walletService.getBalance(getAuthenticatedUser().getUserId()));
    }

    @PostMapping("/send")
    public ResponseEntity<Transaction> transfer(@RequestBody TransactionRequest request) {
        User sender = getAuthenticatedUser();
        return ResponseEntity.ok(walletService.sendMoney(sender.getUserId(), request));
    }

    @PostMapping("/pay-invoice")
    public ResponseEntity<Transaction> payInvoice(@RequestBody InvoicePaymentRequest request) {
        User user = getAuthenticatedUser();
        return ResponseEntity.ok(walletService.payInvoice(
                user.getUserId(),
                request.getInvoiceId(),
                request.getTransactionPin()
        ));
    }

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