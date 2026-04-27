package com.notification.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "notifications",
    indexes = {
        // Optimizes fetching notifications for a user
        @Index(name = "idx_recipient", columnList = "recipientId"),

        // Optimizes filtering unread/read notifications per user
        @Index(name = "idx_recipient_read", columnList = "recipientId, isRead")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    // Primary key (auto-increment)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // User who receives the notification
    @Column(nullable = false)
    private Long recipientId;

    // User who triggered the notification (optional)
    private Long actorId;

    // Type of notification (stored as String for readability)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    // Short title shown in UI
    @Column(nullable = false)
    private String title;

    // Detailed message/content
    @Column(nullable = false)
    private String message;

    // Optional reference to related entity (e.g., task, board)
    private Long relatedId;

    // Type of related entity (can be enum in future)
    private String relatedType;

    // Deep link for frontend navigation
    private String deepLinkUrl;

    // Indicates whether notification has been read
    @Builder.Default
    @Column(nullable = false)
    private boolean isRead = false;

    // Timestamp when notification was read
    private LocalDateTime readAt;

    // Creation timestamp (immutable once set)
    @Builder.Default
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}