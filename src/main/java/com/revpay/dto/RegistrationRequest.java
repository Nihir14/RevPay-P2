package com.revpay.dto;

import lombok.Data;

@Data
public class RegistrationRequest {
    private String email;
    private String password;
    private String fullName;
    private String phoneNumber;
    private String role; // PERSONAL or BUSINESS
    private String transactionPin;
    private String securityQuestion;
    private String securityAnswer;
    
    // Optional Business Fields
    private String businessName;
}