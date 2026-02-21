package com.revpay.controller;

import com.revpay.model.entity.Notification;
import com.revpay.model.entity.User;
import com.revpay.repository.UserRepository;
import com.revpay.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    private MockMvc mockMvc;

    @Mock private NotificationService service;
    @Mock private UserRepository userRepository; // Added to support getAuthenticatedUser()

    @InjectMocks private NotificationController controller;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        // 1. Simulate a logged-in user in the Security Context
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // 2. Mock the JWT email extraction
        lenient().when(authentication.getName()).thenReturn("test@revpay.com");
    }

    // GET /api/notifications
    @Test
    void getMyNotifications_shouldReturnNotificationList() throws Exception {
        // Mock the user lookup
        User mockUser = new User();
        mockUser.setUserId(1L);
        when(userRepository.findByEmail("test@revpay.com")).thenReturn(Optional.of(mockUser));

        Notification n1 = new Notification();
        n1.setMessage("Payment received");

        when(service.getUserNotifications(1L)).thenReturn(List.of(n1));

        mockMvc.perform(get("/api/notifications")) // No ID in URL anymore
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].message").value("Payment received"));

        verify(service).getUserNotifications(1L);
    }

    // PUT /api/notifications/{id}/read
    @Test
    void markAsRead_shouldReturnSuccessMessage() throws Exception {
        doNothing().when(service).markAsRead(10L);

        mockMvc.perform(put("/api/notifications/10/read"))
                .andExpect(status().isOk())
                .andExpect(content().string("Notification marked as read"));

        verify(service).markAsRead(10L);
    }

    // POST /api/notifications/test
    @Test
    void testNotification_shouldCreateNotification() throws Exception {
        // Mock the user lookup
        User mockUser = new User();
        mockUser.setUserId(5L);
        mockUser.setFullName("Test User");
        when(userRepository.findByEmail("test@revpay.com")).thenReturn(Optional.of(mockUser));

        doNothing().when(service).createNotification(anyLong(), anyString(), anyString());

        mockMvc.perform(post("/api/notifications/test")) // No ID in URL anymore
                .andExpect(status().isOk())
                .andExpect(content().string("Test notification created successfully"));

        verify(service).createNotification(
                5L,
                "Test notification from system for: Test User",
                "SYSTEM"
        );
    }
}