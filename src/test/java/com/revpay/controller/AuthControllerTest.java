package com.revpay.controller;

import com.revpay.dto.JwtResponse;
import com.revpay.model.dto.LoginRequest;
import com.revpay.model.dto.SignupRequest;
import com.revpay.model.entity.Role;
import com.revpay.model.entity.User;
import com.revpay.repository.UserRepository;
import com.revpay.security.JwtUtils;
import com.revpay.security.UserDetailsImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder encoder;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private AuthController authController;

    @Test
    void authenticateUserSuccess() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@revpay.com");
        loginRequest.setPassword("password");

        Authentication authentication = mock(Authentication.class);
        UserDetailsImpl userDetails = new UserDetailsImpl(
                1L,
                "test@revpay.com",
                "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_PERSONAL"))
        );

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(jwtUtils.generateTokenFromUsername("test@revpay.com")).thenReturn("mockedJwtToken");

        ResponseEntity<?> response = authController.authenticateUser(loginRequest);

        assertEquals(200, response.getStatusCodeValue());
        JwtResponse jwtResponse = (JwtResponse) response.getBody();
        assertNotNull(jwtResponse);
        assertEquals("mockedJwtToken", jwtResponse.getToken());
        assertEquals("test@revpay.com", jwtResponse.getEmail());
        assertEquals("ROLE_PERSONAL", jwtResponse.getRole());
    }

    @Test
    void registerUserSuccess() {
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setEmail("new@revpay.com");
        signupRequest.setPhoneNumber("1234567890");
        signupRequest.setPassword("password");
        signupRequest.setTransactionPin("1234");
        signupRequest.setFullName("Test User");
        signupRequest.setRole("personal");

        when(userRepository.existsByEmail("new@revpay.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("1234567890")).thenReturn(false);
        when(encoder.encode("password")).thenReturn("encodedPassword");
        when(encoder.encode("1234")).thenReturn("encodedPin");

        ResponseEntity<?> response = authController.registerUser(signupRequest);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("User registered successfully!", response.getBody());
    }

    @Test
    void registerUserEmailExists() {
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setEmail("existing@revpay.com");

        when(userRepository.existsByEmail("existing@revpay.com")).thenReturn(true);

        ResponseEntity<?> response = authController.registerUser(signupRequest);

        assertEquals(400, response.getStatusCodeValue());
        assertEquals("Error: Email is already in use!", response.getBody());
    }
}