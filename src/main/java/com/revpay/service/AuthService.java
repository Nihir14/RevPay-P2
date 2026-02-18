package com.revpay.service;

import com.revpay.dto.RegistrationRequest;
import com.revpay.exception.UserAlreadyExistsException;
import com.revpay.model.*;
import com.revpay.repository.*;
import jakarta.transaction.Transactional;
import org.slf4j.Logger; // Corrected Import
import org.slf4j.LoggerFactory; // Corrected Import
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class AuthService {
    // Corrected Logger initialization
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired private UserRepository userRepository;
    @Autowired private WalletRepository walletRepository;
    @Autowired private BusinessProfileRepository businessRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Transactional
    public void registerUser(RegistrationRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email is already registered!");
        }
        logger.info("REGISTRATION_START | Email: {}", request.getEmail());

        // 1. Create User
        User user = new User();
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setRole(Role.valueOf(request.getRole())); // Ensure Role Enum matches SQL
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setTransactionPinHash(passwordEncoder.encode(request.getTransactionPin()));
        user.setSecurityQuestion(request.getSecurityQuestion());
        user.setSecurityAnswerHash(passwordEncoder.encode(request.getSecurityAnswer()));

        User savedUser = userRepository.save(user);

        // 2. Initialize Wallet (Crucial for Member 2)
        Wallet wallet = new Wallet();
        wallet.setUser(savedUser);
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setCurrency("INR");
        walletRepository.save(wallet);

        // 3. Initialize Business Profile if needed (Crucial for Member 3/4)
        if (savedUser.getRole() == Role.BUSINESS) {
            BusinessProfile profile = new BusinessProfile();
            profile.setUser(savedUser);
            profile.setBusinessName(request.getBusinessName());
            businessRepository.save(profile);
        }

        logger.info("REGISTRATION_SUCCESS | UserID: {} | Wallet Created", savedUser.getUserId());
    }
}