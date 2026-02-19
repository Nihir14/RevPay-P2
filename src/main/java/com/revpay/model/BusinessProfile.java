package com.revpay.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "business_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusinessProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long profileId;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 100)
    private String businessName;

    @Column(length = 50)
    private String businessType;

    @Column(unique = true, length = 50)
    private String taxId;

    @Column(columnDefinition = "TEXT")
    private String address;

    private String verificationDocUrl;
}