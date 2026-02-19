package com.revpay.notification.controller;

import com.revpay.notification.entity.Notification;
import com.revpay.notification.service.NotificationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    // get all notifications of a user
    @GetMapping("/{userId}")
    public List<Notification> getNotifications(@PathVariable Long userId) {
        return service.getUserNotifications(userId);
    }

    // mark notification as read
    @PutMapping("/{id}/read")
    public String markAsRead(@PathVariable Long id) {
        service.markAsRead(id);
        return "Notification marked as read";
    }
    //temporary test api
    @PostMapping("/test/{userId}")
    public String testNotification(@PathVariable Long userId) {
        service.createNotification(
                userId,
                "Test notification from Postman",
                "SYSTEM"
        );
        return "Test notification created";
    }
}
