package com.revpay.service;

import com.revpay.model.entity.*;
import com.revpay.repository.*;
import com.revpay.model.dto.TransactionRequest;
import com.revpay.model.dto.WalletAnalyticsDTO; // New DTO for Feature 4
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class WalletService {

    private static final Logger logger = LoggerFactory.getLogger(WalletService.class);

    @Autowired private WalletRepository walletRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private PaymentMethodRepository paymentMethodRepository;
    @Autowired private InvoiceRepository invoiceRepository;

    // --- FEATURE 1: AUDIT REFERENCE GENERATOR ---
    private String generateRef() {
        return "TXN-" + System.currentTimeMillis();
    }

    // --- FEATURE 2: DAILY LIMIT CHECK (₹50,000) ---
    private void checkDailyLimit(User sender, BigDecimal newAmount) {
        BigDecimal dailyLimit = new BigDecimal("50000.00");
        LocalDateTime startOfDay = LocalDateTime.now().with(LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.now().with(LocalTime.MAX);

        BigDecimal totalSentToday = transactionRepository.findBySenderOrReceiverOrderByTimestampDesc(sender, sender)
                .stream()
                .filter(t -> t.getSender() != null && t.getSender().getUserId().equals(sender.getUserId()))
                .filter(t -> t.getType() == Transaction.TransactionType.SEND)
                .filter(t -> t.getTimestamp().isAfter(startOfDay) && t.getTimestamp().isBefore(endOfDay))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalSentToday.add(newAmount).compareTo(dailyLimit) > 0) {
            throw new RuntimeException("Daily transfer limit of ₹50,000 exceeded! Already sent: ₹" + totalSentToday);
        }
    }

    // --- FEATURE 4: WALLET ANALYTICS (SPEND CATEGORIZATION) ---
    public WalletAnalyticsDTO getSpendingAnalytics(User user) {
        List<Transaction> history = transactionRepository.findBySenderOrReceiverOrderByTimestampDesc(user, user);

        // Filter for money leaving the wallet (where user is the sender and status is COMPLETED)
        List<Transaction> outgoing = history.stream()
                .filter(t -> t.getSender() != null && t.getSender().getUserId().equals(user.getUserId()))
                .filter(t -> t.getStatus() == Transaction.TransactionStatus.COMPLETED)
                .collect(Collectors.toList());

        BigDecimal totalSpent = outgoing.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Group by Type (SEND, INVOICE_PAYMENT, WITHDRAW) and sum amounts
        Map<String, BigDecimal> spendingByCategory = outgoing.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getType().name(),
                        Collectors.mapping(Transaction::getAmount,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ));

        return new WalletAnalyticsDTO(totalSpent, spendingByCategory, (long) outgoing.size());
    }

    // --- FEATURE 3: WALLET-TO-INVOICE PAYMENT ---
    @Transactional
    public Transaction payInvoice(Long userId, Long invoiceId, String pin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(pin, user.getTransactionPinHash())) {
            throw new RuntimeException("Invalid Transaction PIN!");
        }

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        if (invoice.getStatus() == Invoice.InvoiceStatus.PAID) {
            throw new RuntimeException("Invoice is already paid!");
        }

        Wallet userWallet = walletRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        if (userWallet.getBalance().compareTo(invoice.getTotalAmount()) < 0) {
            throw new RuntimeException("Insufficient balance to pay this invoice!");
        }

        userWallet.setBalance(userWallet.getBalance().subtract(invoice.getTotalAmount()));
        walletRepository.save(userWallet);

        invoice.setStatus(Invoice.InvoiceStatus.PAID);
        invoiceRepository.save(invoice);

        Transaction transaction = new Transaction();
        transaction.setSender(user);
        transaction.setReceiver(invoice.getBusinessProfile().getUser());
        transaction.setAmount(invoice.getTotalAmount());
        transaction.setType(Transaction.TransactionType.INVOICE_PAYMENT);
        transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
        transaction.setDescription("Settled Invoice #" + invoice.getId());
        transaction.setTransactionRef(generateRef());

        logger.info("INVOICE_PAID | User: {} | Invoice ID: {}", user.getEmail(), invoice.getId());
        return transactionRepository.save(transaction);
    }

    // --- 1. BALANCE & HISTORY ---
    public BigDecimal getBalance(Long userId) {
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
        User sender = userRepository.findById(senderId).orElseThrow(() -> new RuntimeException("Sender not found"));
        User receiver = userRepository.findByEmail(request.getReceiverIdentifier()).orElseThrow(() -> new RuntimeException("Receiver not found"));

        if (!passwordEncoder.matches(request.getTransactionPin(), sender.getTransactionPinHash())) {
            throw new RuntimeException("Invalid Transaction PIN!");
        }

        checkDailyLimit(sender, request.getAmount());

        Wallet senderWallet = walletRepository.findById(senderId).orElseThrow(() -> new RuntimeException("Sender wallet not found"));
        Wallet receiverWallet = walletRepository.findById(receiver.getUserId()).orElseThrow(() -> new RuntimeException("Receiver wallet not found"));

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
        transaction.setTransactionRef(generateRef());

        return transactionRepository.save(transaction);
    }

    @Transactional
    public Transaction addFunds(Long userId, BigDecimal amount, String description) {
        Wallet wallet = walletRepository.findById(userId).orElseThrow(() -> new RuntimeException("Wallet not found"));
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        Transaction transaction = new Transaction();
        transaction.setReceiver(wallet.getUser());
        transaction.setAmount(amount);
        transaction.setType(Transaction.TransactionType.ADD_FUNDS);
        transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
        transaction.setDescription(description);
        transaction.setTransactionRef(generateRef());

        return transactionRepository.save(transaction);
    }

    @Transactional
    public Transaction withdrawFunds(Long userId, BigDecimal amount) {
        Wallet wallet = walletRepository.findById(userId).orElseThrow(() -> new RuntimeException("Wallet not found"));
        if (wallet.getBalance().compareTo(amount) < 0) throw new RuntimeException("Insufficient balance");

        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);

        Transaction transaction = new Transaction();
        transaction.setSender(wallet.getUser());
        transaction.setAmount(amount);
        transaction.setType(Transaction.TransactionType.WITHDRAW);
        transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
        transaction.setTransactionRef(generateRef());

        return transactionRepository.save(transaction);
    }

    // --- 3. MONEY REQUESTS ---
    @Transactional
    public Transaction requestMoney(Long requesterId, String targetEmail, BigDecimal amount) {
        User requester = userRepository.findById(requesterId).orElseThrow(() -> new RuntimeException("Requester not found"));
        User target = userRepository.findByEmail(targetEmail).orElseThrow(() -> new RuntimeException("Target user not found"));

        Transaction request = new Transaction();
        request.setSender(target);
        request.setReceiver(requester);
        request.setAmount(amount);
        request.setType(Transaction.TransactionType.REQUEST);
        request.setStatus(Transaction.TransactionStatus.PENDING);
        request.setDescription("Money request from " + requester.getFullName());
        request.setTransactionRef(generateRef());

        return transactionRepository.save(request);
    }

    @Transactional
    public Transaction acceptRequest(Long transactionId, String pin) {
        Transaction request = transactionRepository.findById(transactionId).orElseThrow(() -> new RuntimeException("Request not found"));
        TransactionRequest transReq = new TransactionRequest(request.getReceiver().getEmail(), request.getAmount(), "Accepted Request", pin);
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