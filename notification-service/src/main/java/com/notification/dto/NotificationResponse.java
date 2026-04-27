package com.notification.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

import com.notification.entity.NotificationType;

@Data
@Builder
public class NotificationResponse {

    // Unique identifier of the notification
    private Long id;

    private Long recipientId;
    
    private Long actorId;

    private NotificationType type;

    private String title;

    private String message;

    private Long relatedId;

    private String relatedType;

    private String deepLinkUrl;

    private boolean isRead;

    private LocalDateTime createdAt;

    private LocalDateTime readAt;
}