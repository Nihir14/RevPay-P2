package com.revpay;

import com.revpay.model.dto.RegistrationRequest;
import com.revpay.model.entity.User;
import com.revpay.model.entity.Wallet;
import com.revpay.repository.UserRepository;
import com.revpay.repository.WalletRepository;
import com.revpay.service.AuthService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@SpringBootTest
@Transactional
public class RegistrationIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Test
    void testCompleteRegistrationFlow() {
        // Use a timestamp to ensure unique email and phone for every test run
        long timestamp = System.currentTimeMillis();
        String uniqueEmail = "test" + timestamp + "@revpay.com";
        String uniquePhone = String.valueOf(timestamp).substring(0, 10); // Ensures 10 digits

        // 1. Prepare Request Data
        RegistrationRequest req = new RegistrationRequest();
        req.setEmail(uniqueEmail);
        req.setPassword("pass123");
        req.setFullName("Test User");
        req.setPhoneNumber(uniquePhone);
        req.setRole("PERSONAL");
        req.setTransactionPin("1122");
        req.setSecurityQuestion("Your City?");
        req.setSecurityAnswer("Salem");

        // 2. Execute Action
        authService.registerUser(req);

        // 3. Verify User was saved
        Optional<User> savedUser = userRepository.findByEmail(uniqueEmail);
        assertTrue(savedUser.isPresent(), "User should be present in the database");

        // 4. Verify Wallet was initialized (Linked via User ID)
        Optional<Wallet> savedWallet = walletRepository.findById(savedUser.get().getUserId());
        assertTrue(savedWallet.isPresent(), "Wallet should be automatically created for the user");

        assertEquals(0, savedWallet.get().getBalance().compareTo(java.math.BigDecimal.ZERO),
                "Initial wallet balance should be zero");
    }
}