package com.revpay.service;

import com.revpay.model.entity.Notification;
import com.revpay.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private static final Logger log =
            LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    // called by other modules
    public void createNotification(Long userId, String message, String type) {

        log.info(
                "Creating notification for userId={} with type={}",
                userId, type
        );

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setMessage(message);
        notification.setType(type);

        repository.save(notification);

        log.info(
                "Notification created successfully for userId={} with message={}",
                userId, message
        );
    }

    // fetch notifications for UI
    public List<Notification> getUserNotifications(Long userId) {

        log.info("Fetching notifications for userId={}", userId);

        List<Notification> notifications =
                repository.findByUserIdOrderByCreatedAtDesc(userId);

        log.debug(
                "Fetched {} notifications for userId={}",
                notifications.size(), userId
        );

        return notifications;
    }

    // mark as read
    public void markAsRead(Long notificationId) {

        log.info(
                "Marking notification as read for notificationId={}",
                notificationId
        );

        Notification notification = repository.findById(notificationId)
                .orElseThrow(() -> {
                    log.error(
                            "Notification not found for notificationId={}",
                            notificationId
                    );
                    return new RuntimeException("Notification not found");
                });

        notification.setRead(true);
        repository.save(notification);

        log.info(
                "Notification marked as read successfully for notificationId={}",
                notificationId
        );
    }
}