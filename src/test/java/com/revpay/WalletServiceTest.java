package com.revpay;

import com.revpay.model.dto.TransactionRequest;
import com.revpay.model.entity.*;
import com.revpay.repository.*;
import com.revpay.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WalletServiceTest {

    @Mock private WalletRepository walletRepo;
    @Mock private TransactionRepository transRepo;
    @Mock private UserRepository userRepo;
    @Mock private PasswordEncoder encoder;

    @InjectMocks
    private WalletService walletService;

    private User sender;
    private Wallet senderWallet;

    @BeforeEach
    void setup() {
        // Common setup for all tests
        sender = new User();
        sender.setUserId(1L);
        sender.setEmail("sender@revpay.com");
        sender.setTransactionPinHash("hashed_1234");

        senderWallet = new Wallet();
        senderWallet.setBalance(new BigDecimal("1000.00"));
        senderWallet.setUser(sender);
    }

    // --- 1. SUCCESS CASE: SEND MONEY ---
    @Test
    void testSendMoney_Success() {
        TransactionRequest req = new TransactionRequest("receiver@mail.com", new BigDecimal("200.00"), "Lunch", "1234");
        User receiver = new User();
        receiver.setUserId(2L);
        Wallet receiverWallet = new Wallet();
        receiverWallet.setBalance(new BigDecimal("50.00"));

        when(userRepo.findById(1L)).thenReturn(Optional.of(sender));
        when(userRepo.findByEmail("receiver@mail.com")).thenReturn(Optional.of(receiver));
        when(encoder.matches("1234", sender.getTransactionPinHash())).thenReturn(true);
        when(walletRepo.findById(1L)).thenReturn(Optional.of(senderWallet));
        when(walletRepo.findById(2L)).thenReturn(Optional.of(receiverWallet));

        walletService.sendMoney(1L, req);

        assertEquals(new BigDecimal("800.00"), senderWallet.getBalance());
        assertEquals(new BigDecimal("250.00"), receiverWallet.getBalance());
        verify(walletRepo, times(2)).save(any(Wallet.class));
    }

    // --- 2. SECURITY CASE: WRONG PIN ---
    @Test
    void testSendMoney_WrongPin_ShouldFail() {
        TransactionRequest req = new TransactionRequest("r@mail.com", new BigDecimal("100.00"), "test", "wrong_pin");

        when(userRepo.findById(1L)).thenReturn(Optional.of(sender));
        when(encoder.matches("wrong_pin", sender.getTransactionPinHash())).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> walletService.sendMoney(1L, req));
        assertEquals("Invalid Transaction PIN!", ex.getMessage());
    }

    // --- 3. LOGIC CASE: INSUFFICIENT BALANCE ---
    @Test
    void testSendMoney_LowBalance_ShouldFail() {
        // Trying to send 5000 when only 1000 is available
        TransactionRequest req = new TransactionRequest("r@mail.com", new BigDecimal("5000.00"), "broke", "1234");

        when(userRepo.findById(1L)).thenReturn(Optional.of(sender));
        when(userRepo.findByEmail(anyString())).thenReturn(Optional.of(new User()));
        when(encoder.matches(anyString(), anyString())).thenReturn(true);
        when(walletRepo.findById(1L)).thenReturn(Optional.of(senderWallet));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> walletService.sendMoney(1L, req));
        assertEquals("Insufficient balance!", ex.getMessage());
    }

    // --- 4. GUARDRAIL CASE: DAILY LIMIT BREACH ---
    @Test
    void testDailyLimit_Exceeded_ShouldFail() {
        // Requesting 50,001 (Limit is 50,000)
        TransactionRequest req = new TransactionRequest("r@mail.com", new BigDecimal("50001.00"), "OverLimit", "1234");

        when(userRepo.findById(1L)).thenReturn(Optional.of(sender));
        when(encoder.matches(anyString(), anyString())).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> walletService.sendMoney(1L, req));
        assertTrue(ex.getMessage().contains("limit of ₹50,000 exceeded"));
    }

    // --- 5. AUDIT CASE: REFERENCE FORMAT ---
    @Test
    void testTransactionReference_Format() {
        when(walletRepo.findById(1L)).thenReturn(Optional.of(senderWallet));
        when(transRepo.save(any())).thenAnswer(invocation -> invocation.getArguments()[0]);

        Transaction result = walletService.addFunds(1L, new BigDecimal("100.00"), "Bonus");

        assertNotNull(result.getTransactionRef());
        assertTrue(result.getTransactionRef().startsWith("TXN-"), "Ref should start with TXN-");
    }



}