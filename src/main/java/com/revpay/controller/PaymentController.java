package com.revpay.controller;

import com.revpay.model.PaymentMethod;
import com.revpay.model.User;
import com.revpay.repository.UserRepository;
import com.revpay.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
public class PaymentController {

    @Autowired private WalletService walletService;
    @Autowired private UserRepository userRepository;

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        // Using .orElseThrow() fixes your yellow "Optional.get()" warning
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }

    @PostMapping("/add")
    public ResponseEntity<PaymentMethod> addCard(@RequestBody PaymentMethod card) {
        return ResponseEntity.ok(walletService.addCard(getAuthenticatedUser().getUserId(), card));
    }

    @GetMapping("/my-cards")
    public ResponseEntity<List<PaymentMethod>> getMyCards() {
        return ResponseEntity.ok(walletService.getCards(getAuthenticatedUser().getUserId()));
    }

    @PatchMapping("/{cardId}/default")
    public ResponseEntity<String> makeDefault(@PathVariable Long cardId) {
        walletService.setDefaultCard(getAuthenticatedUser().getUserId(), cardId);
        return ResponseEntity.ok("Default card updated");
    }
}