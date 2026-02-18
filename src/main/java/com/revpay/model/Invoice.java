package com.revpay.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "invoices")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "business_id", nullable = false)
    private BusinessProfile businessProfile;

    private String customerName;
    private String customerEmail;
    private BigDecimal totalAmount;
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    private InvoiceStatus status; // Requirements: DRAFT, SENT, PAID, etc.

    public enum InvoiceStatus {
        DRAFT, SENT, PAID, OVERDUE, CANCELLED
    }
}
