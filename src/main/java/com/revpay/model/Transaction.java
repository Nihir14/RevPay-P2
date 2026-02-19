package com.revpay.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionId;

    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = true) // Null for "Add Funds"
    private User sender;

    @ManyToOne
    @JoinColumn(name = "receiver_id", nullable = true) // Null for "Withdraw"
    private User receiver;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    private String description;

    @CreationTimestamp
    private LocalDateTime timestamp;

    public enum TransactionType {
        SEND, REQUEST, ADD_FUNDS, WITHDRAW, INVOICE_PAYMENT
    }

    public enum TransactionStatus {
        PENDING, COMPLETED, FAILED, DECLINED
    }
}