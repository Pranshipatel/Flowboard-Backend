package com.notification.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

import com.notification.entity.NotificationType;

@Data
public class SendBulkNotificationRequest {

    @NotEmpty(message = "Recipient list must not be empty")
    private List<Long> recipientIds;

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
}