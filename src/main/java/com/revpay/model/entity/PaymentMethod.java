package com.revpay.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "payment_methods")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String cardNumber; // Should be masked in real scenarios
    private String expiryDate; // MM/YY
    private String cvv; // For simulation purposes
    private String billingAddress;
    private boolean isDefault = false;

    @Enumerated(EnumType.STRING)
    private CardType cardType; // VISA, MASTERCARD, etc.

    public enum CardType {
        VISA, MASTERCARD, AMEX, DISCOVER
    }
}
