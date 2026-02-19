package com.revpay.controller;

import com.revpay.model.Transaction;
import com.revpay.model.User;
import com.revpay.repository.UserRepository;
import com.revpay.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    @Autowired private WalletService walletService;
    @Autowired private UserRepository userRepository;

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElseThrow();
    }

    @GetMapping("/history")
    public ResponseEntity<List<Transaction>> getHistory(@RequestParam(required = false) String type) {
        return ResponseEntity.ok(walletService.getMyHistory(getAuthenticatedUser(), type));
    }

    @PostMapping("/request")
    public ResponseEntity<Transaction> requestMoney(@RequestParam String targetEmail, @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(walletService.requestMoney(getAuthenticatedUser().getUserId(), targetEmail, amount));
    }

    @PostMapping("/request/{id}/accept")
    public ResponseEntity<Transaction> accept(@PathVariable Long id, @RequestParam String pin) {
        return ResponseEntity.ok(walletService.acceptRequest(id, pin));
    }
}