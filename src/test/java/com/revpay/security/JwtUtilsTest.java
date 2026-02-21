package com.revpay.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {

    private JwtUtils jwtUtils;
    private final String testEmail = "test@revpay.com";

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 86400000);
    }

    @Test
    void generateAndValidateToken() {
        String token = jwtUtils.generateTokenFromUsername(testEmail);
        assertNotNull(token);

        boolean isValid = jwtUtils.validateJwtToken(token);
        assertTrue(isValid);
    }

    @Test
    void extractUsernameFromToken() {
        String token = jwtUtils.generateTokenFromUsername(testEmail);
        String extractedEmail = jwtUtils.getUserNameFromJwtToken(token);

        assertEquals(testEmail, extractedEmail);
    }

    @Test
    void validateTokenFailure() {
        String fakeToken = "eyJhbGciOiJIUzI1NiJ9.fakePayload.fakeSignature";
        boolean isValid = jwtUtils.validateJwtToken(fakeToken);

        assertFalse(isValid);
    }
}