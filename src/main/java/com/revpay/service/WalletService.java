package com.revpay.service;

import com.revpay.model.entity.*;
import com.revpay.repository.*;
import com.revpay.model.dto.TransactionRequest;
import com.revpay.model.dto.WalletAnalyticsDTO;
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

    // --- 1. HELPERS & SECURITY ---

    private String generateRef() {
        return "TXN-" + System.currentTimeMillis();
    }

    private void checkDailyLimit(User sender, BigDecimal newAmount) {
        BigDecimal dailyLimit = new BigDecimal("50000.00");
        LocalDateTime startOfDay = LocalDateTime.now().with(LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.now().with(LocalTime.MAX);

        BigDecimal totalSentToday = transactionRepository.findBySenderOrReceiverOrderByTimestampDesc(sender, sender)
                .stream()
                .filter(t -> t.getSender() != null && t.getSender().getUserId().equals(sender.getUserId()))
                .filter(t -> t.getType() == Transaction.TransactionType.SEND)
                .filter(t -> t.getStatus() == Transaction.TransactionStatus.COMPLETED)
                .filter(t -> t.getTimestamp().isAfter(startOfDay) && t.getTimestamp().isBefore(endOfDay))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalSentToday.add(newAmount).compareTo(dailyLimit) > 0) {
            logger.warn("SECURITY | LIMIT_EXCEEDED | User: {} | Used: {}", sender.getEmail(), totalSentToday);
            throw new RuntimeException("Daily transfer limit of ₹50,000 exceeded!");
        }
    }

    // --- 2. TRANSACTION HISTORY ---

    public List<Transaction> getTransactionHistory(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        return transactionRepository.findBySenderOrReceiverOrderByTimestampDesc(user, user);
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

    // --- 3. MONEY REQUESTS ---

    @Transactional
    public Transaction requestMoney(Long requesterId, String targetEmail, BigDecimal amount) {
        User requester = userRepository.findById(requesterId).orElseThrow(() -> new RuntimeException("Requester not found"));
        User target = userRepository.findByEmail(targetEmail).orElseThrow(() -> new RuntimeException("Target user not found"));

        Transaction request = new Transaction();
        request.setSender(target); // Person who owes the money
        request.setReceiver(requester);
        request.setAmount(amount);
        request.setType(Transaction.TransactionType.REQUEST);
        request.setStatus(Transaction.TransactionStatus.PENDING);
        request.setTransactionRef(generateRef());

        logger.info("REQUEST | CREATED | From: {} | To: {}", requester.getEmail(), targetEmail);
        return transactionRepository.save(request);
    }

    @Transactional
    public Transaction acceptRequest(Long transactionId, String pin) {
        Transaction request = transactionRepository.findById(transactionId).orElseThrow(() -> new RuntimeException("Request not found"));

        // Use sendMoney logic to fulfill the request
        TransactionRequest paymentReq = new TransactionRequest(request.getReceiver().getEmail(), request.getAmount(), "Payment for Request", pin);
        sendMoney(request.getSender().getUserId(), paymentReq);

        request.setStatus(Transaction.TransactionStatus.COMPLETED);
        return transactionRepository.save(request);
    }

    // --- 4. CORE WALLET OPERATIONS ---

    public BigDecimal getBalance(Long userId) {
        return walletRepository.findById(userId).map(Wallet::getBalance).orElseThrow(() -> new RuntimeException("Wallet not found"));
    }

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

        Transaction tx = new Transaction();
        tx.setSender(sender);
        tx.setReceiver(receiver);
        tx.setAmount(request.getAmount());
        tx.setType(Transaction.TransactionType.SEND);
        tx.setStatus(Transaction.TransactionStatus.COMPLETED);
        tx.setTransactionRef(generateRef());

        logger.info("TRANSACTION | SUCCESS | From: {} | To: {} | Ref: {}", sender.getEmail(), receiver.getEmail(), tx.getTransactionRef());
        return transactionRepository.save(tx);
    }

    @Transactional
    public Transaction addFunds(Long userId, BigDecimal amount, String description) {
        Wallet wallet = walletRepository.findById(userId).orElseThrow(() -> new RuntimeException("Wallet not found"));
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        Transaction tx = new Transaction();
        tx.setReceiver(wallet.getUser());
        tx.setAmount(amount);
        tx.setType(Transaction.TransactionType.ADD_FUNDS);
        tx.setStatus(Transaction.TransactionStatus.COMPLETED);
        tx.setTransactionRef(generateRef());

        logger.info("WALLET_UPDATE | DEPOSIT | User: {} | Amount: {}", wallet.getUser().getEmail(), amount);
        return transactionRepository.save(tx);
    }

    @Transactional
    public Transaction withdrawFunds(Long userId, BigDecimal amount) {
        Wallet wallet = walletRepository.findById(userId).orElseThrow(() -> new RuntimeException("Wallet not found"));
        if (wallet.getBalance().compareTo(amount) < 0) throw new RuntimeException("Insufficient balance");

        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);

        Transaction tx = new Transaction();
        tx.setSender(wallet.getUser());
        tx.setAmount(amount);
        tx.setType(Transaction.TransactionType.WITHDRAW);
        tx.setStatus(Transaction.TransactionStatus.COMPLETED);
        tx.setTransactionRef(generateRef());

        logger.info("WALLET_UPDATE | WITHDRAWAL | User: {} | Amount: {}", wallet.getUser().getEmail(), amount);
        return transactionRepository.save(tx);
    }

    @Transactional
    public Transaction payInvoice(Long userId, Long invoiceId, String pin) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        if (!passwordEncoder.matches(pin, user.getTransactionPinHash())) throw new RuntimeException("Invalid PIN!");

        Invoice invoice = invoiceRepository.findById(invoiceId).orElseThrow(() -> new RuntimeException("Invoice not found"));
        Wallet wallet = walletRepository.findById(userId).orElseThrow(() -> new RuntimeException("Wallet not found"));

        if (wallet.getBalance().compareTo(invoice.getTotalAmount()) < 0) throw new RuntimeException("Insufficient balance!");

        wallet.setBalance(wallet.getBalance().subtract(invoice.getTotalAmount()));
        walletRepository.save(wallet);
        invoice.setStatus(Invoice.InvoiceStatus.PAID);
        invoiceRepository.save(invoice);

        Transaction tx = new Transaction();
        tx.setSender(user);
        tx.setReceiver(invoice.getBusinessProfile().getUser());
        tx.setAmount(invoice.getTotalAmount());
        tx.setType(Transaction.TransactionType.INVOICE_PAYMENT);
        tx.setStatus(Transaction.TransactionStatus.COMPLETED);
        tx.setTransactionRef(generateRef());

        return transactionRepository.save(tx);
    }

    // --- 5. CARD MANAGEMENT ---

    public PaymentMethod addCard(Long userId, PaymentMethod card) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        card.setUser(user);
        logger.info("CARD_SERVICE | ADDED | User: {}", user.getEmail());
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
        cards.forEach(c -> c.setDefault(c.getId().equals(cardId)));
        paymentMethodRepository.saveAll(cards);
        logger.info("CARD_SERVICE | DEFAULT_SET | User: {} | CardID: {}", user.getEmail(), cardId);
    }

    // --- 6. ANALYTICS ---

    public WalletAnalyticsDTO getSpendingAnalytics(User user) {
        List<Transaction> history = transactionRepository.findBySenderOrReceiverOrderByTimestampDesc(user, user);
        List<Transaction> outgoing = history.stream()
                .filter(t -> t.getSender() != null && t.getSender().getUserId().equals(user.getUserId()))
                .filter(t -> t.getStatus() == Transaction.TransactionStatus.COMPLETED)
                .collect(Collectors.toList());

        BigDecimal totalSpent = outgoing.stream().map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, BigDecimal> categories = outgoing.stream()
                .collect(Collectors.groupingBy(t -> t.getType().name(),
                        Collectors.mapping(Transaction::getAmount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));

        return new WalletAnalyticsDTO(totalSpent, categories, (long) outgoing.size());
    }
}