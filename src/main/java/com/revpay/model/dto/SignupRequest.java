package com.revpay.model.dto;

import lombok.Data;

@Data
public class SignupRequest {
    private String email;
    private String phoneNumber;
    private String password;
    private String transactionPin;
    private String fullName;
    private String role;
    private String securityQuestion;
    private String securityAnswer;
}