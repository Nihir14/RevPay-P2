package com.revpay.controller;

import com.revpay.model.entity.Notification;
import com.revpay.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private NotificationService service;

    @InjectMocks
    private NotificationController controller;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();
    }


    // GET /notifications/{userId}

    @Test
    void getNotifications_shouldReturnNotificationList() throws Exception {

        Notification n1 = new Notification();
        n1.setMessage("Payment received");

        Notification n2 = new Notification();
        n2.setMessage("Loan approved");

        when(service.getUserNotifications(1L))
                .thenReturn(List.of(n1, n2));

        mockMvc.perform(get("/notifications/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].message").value("Payment received"));

        verify(service).getUserNotifications(1L);
    }


    // PUT /notifications/{id}/read

    @Test
    void markAsRead_shouldReturnSuccessMessage() throws Exception {

        doNothing().when(service).markAsRead(10L);

        mockMvc.perform(put("/notifications/10/read"))
                .andExpect(status().isOk())
                .andExpect(content().string("Notification marked as read"));

        verify(service).markAsRead(10L);
    }


    // POST /notifications/test/{id}

    @Test
    void testNotification_shouldCreateNotification() throws Exception {

        doNothing().when(service)
                .createNotification(anyLong(), anyString(), anyString());

        mockMvc.perform(post("/notifications/test/5"))
                .andExpect(status().isOk())
                .andExpect(content().string("Test notification created"));

        verify(service).createNotification(
                5L,
                "Test notification from Postman",
                "SYSTEM"
        );
    }
}