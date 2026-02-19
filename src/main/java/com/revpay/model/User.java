package com.revpay.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(unique = true, nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String transactionPinHash;

    private String fullName;

    @Enumerated(EnumType.STRING)
    private Role role; // Enum: PERSONAL, BUSINESS, ADMIN

    private String securityQuestion;
    private String securityAnswerHash;

    private boolean isActive = true;
    private boolean isVerified = false;

    @CreationTimestamp
    private LocalDateTime createdAt;
}