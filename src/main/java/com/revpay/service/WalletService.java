package com.revpay.service;

import com.revpay.model.*;
import com.revpay.repository.*;
import com.revpay.dto.TransactionRequest;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WalletService {

    private static final Logger logger = LoggerFactory.getLogger(WalletService.class);

    @Autowired private WalletRepository walletRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private PaymentMethodRepository paymentMethodRepository;

    // --- 1. BALANCE & HISTORY ---
    public BigDecimal getBalance(Long userId) {
        // Safe check using orElseThrow
        Wallet wallet = walletRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found for User ID: " + userId));
        return wallet.getBalance();
    }

    public List<Transaction> getMyHistory(User user, String type) {
        List<Transaction> all = transactionRepository.findBySenderOrReceiverOrderByTimestampDesc(user, user);
        if (type != null && !type.isEmpty()) {
            return all.stream()
                    .filter(t -> t.getType().name().equalsIgnoreCase(type))
                    .collect(Collectors.toList());
        }
        return all;
    }

    // --- 2. CORE TRANSACTIONS ---
    @Transactional
    public Transaction sendMoney(Long senderId, TransactionRequest request) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Sender not found"));
        User receiver = userRepository.findByEmail(request.getReceiverIdentifier())
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

        if (!passwordEncoder.matches(request.getTransactionPin(), sender.getTransactionPinHash())) {
            throw new RuntimeException("Invalid Transaction PIN!");
        }

        // Updated from .get() to orElseThrow to prevent crashes
        Wallet senderWallet = walletRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Sender wallet not found"));
        Wallet receiverWallet = walletRepository.findById(receiver.getUserId())
                .orElseThrow(() -> new RuntimeException("Receiver wallet not found"));

        if (senderWallet.getBalance().compareTo(request.getAmount()) < 0) {
            throw new RuntimeException("Insufficient balance!");
        }

        senderWallet.setBalance(senderWallet.getBalance().subtract(request.getAmount()));
        receiverWallet.setBalance(receiverWallet.getBalance().add(request.getAmount()));

        walletRepository.save(senderWallet);
        walletRepository.save(receiverWallet);

        Transaction transaction = new Transaction();
        transaction.setSender(sender);
        transaction.setReceiver(receiver);
        transaction.setAmount(request.getAmount());
        transaction.setType(Transaction.TransactionType.SEND);
        transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
        transaction.setDescription(request.getDescription());

        logger.info("TRANSFER_SUCCESS | From: {} | To: {}", sender.getEmail(), receiver.getEmail());
        return transactionRepository.save(transaction);
    }

    @Transactional
    public Transaction addFunds(Long userId, BigDecimal amount, String description) {
        // Updated to orElseThrow
        Wallet wallet = walletRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        Transaction transaction = new Transaction();
        transaction.setReceiver(wallet.getUser());
        transaction.setAmount(amount);
        transaction.setType(Transaction.TransactionType.ADD_FUNDS);
        transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
        transaction.setDescription(description);
        return transactionRepository.save(transaction);
    }

    @Transactional
    public Transaction withdrawFunds(Long userId, BigDecimal amount) {
        // Updated to orElseThrow
        Wallet wallet = walletRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        if (wallet.getBalance().compareTo(amount) < 0) throw new RuntimeException("Insufficient balance");

        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);

        Transaction transaction = new Transaction();
        transaction.setSender(wallet.getUser());
        transaction.setAmount(amount);
        transaction.setType(Transaction.TransactionType.WITHDRAW);
        transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
        return transactionRepository.save(transaction);
    }

    // --- 3. MONEY REQUESTS ---
    @Transactional
    public Transaction requestMoney(Long requesterId, String targetEmail, BigDecimal amount) {
        // Updated to orElseThrow
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new RuntimeException("Requester not found"));
        User target = userRepository.findByEmail(targetEmail)
                .orElseThrow(() -> new RuntimeException("Target user not found"));

        Transaction request = new Transaction();
        request.setSender(target);
        request.setReceiver(requester);
        request.setAmount(amount);
        request.setType(Transaction.TransactionType.REQUEST);
        request.setStatus(Transaction.TransactionStatus.PENDING);
        request.setDescription("Money request from " + requester.getFullName());

        return transactionRepository.save(request);
    }

    @Transactional
    public Transaction acceptRequest(Long transactionId, String pin) {
        Transaction request = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        TransactionRequest transReq = new TransactionRequest(
                request.getReceiver().getEmail(),
                request.getAmount(),
                "Accepted Request",
                pin
        );

        sendMoney(request.getSender().getUserId(), transReq);
        request.setStatus(Transaction.TransactionStatus.COMPLETED);
        return transactionRepository.save(request);
    }

    // --- 4. PAYMENT METHODS ---
    public PaymentMethod addCard(Long userId, PaymentMethod card) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        card.setUser(user);
        return paymentMethodRepository.save(card);
    }

    public List<PaymentMethod> getCards(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        return paymentMethodRepository.findByUser(user);
    }

    @Transactional
    public void setDefaultCard(Long userId, Long cardId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        List<PaymentMethod> cards = paymentMethodRepository.findByUser(user);
        cards.forEach(card -> card.setDefault(card.getId().equals(cardId)));
        paymentMethodRepository.saveAll(cards);
    }
}