package com.notification.dto;


import com.notification.entity.NotificationType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SendNotificationRequest {

    @NotNull(message = "Recipient ID must be provided")
    private Long recipientId;

    private Long actorId;

    @NotNull(message = "Notification type must be provided")
    private NotificationType type;

    @NotBlank(message = "Title must not be blank")
    private String title;

    @NotBlank(message = "Message must not be blank")
    private String message;

    private Long relatedId;

    private String relatedType;

    private String deepLinkUrl;

    private boolean sendEmail = false;

    private String recipientEmail;
}