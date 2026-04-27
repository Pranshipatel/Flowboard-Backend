package com.notification.service;



import java.util.List;

import com.notification.dto.NotificationResponse;
import com.notification.dto.SendBulkNotificationRequest;
import com.notification.dto.SendNotificationRequest;
import com.notification.entity.NotificationType;

public interface NotificationService {

    // Sends a single notification to a user
    NotificationResponse send(SendNotificationRequest request);

    // Sends notifications to multiple recipients in bulk
    List<NotificationResponse> sendBulk(SendBulkNotificationRequest request);


    // Retrieves all notifications for a specific user
    List<NotificationResponse> getByRecipient(Long recipientId);

    // Retrieves only unread notifications for a user
    List<NotificationResponse> getUnreadByRecipient(Long recipientId);

    // Retrieves notifications filtered by type
    List<NotificationResponse> getByRecipientAndType(
            Long recipientId, NotificationType type
    );

    // Retrieves all notifications (admin/debug use)
    List<NotificationResponse> getAll();


    // Marks a specific notification as read
    NotificationResponse markAsRead(Long notificationId, Long recipientId);

    // Marks all notifications as read for a user
    void markAllAsRead(Long recipientId);

    // Returns count of unread notifications
    long getUnreadCount(Long recipientId);


    // Deletes a specific notification (ownership validated)
    void deleteNotification(Long notificationId, Long recipientId);

    // Deletes all read notifications for a user
    void deleteReadNotifications(Long recipientId);


    // Creates assignment notification (optionally triggers email)
    void notifyAssignment(Long recipientId, Long actorId,
                          Long cardId, String cardTitle,
                          String recipientEmail);

    // Creates mention notification when user is tagged
    void notifyMention(Long recipientId, Long actorId,
                       Long cardId, String cardTitle);

    // Notifies user about upcoming due date
    void notifyDueDateApproaching(Long recipientId, Long cardId,
                                  String cardTitle, String timeLeft);

    // Notifies user when card is moved to "Done"
    void notifyCardMovedToDone(Long recipientId, Long actorId,
                               Long cardId, String cardTitle);

    // Notifies user about replies on their comment
    void notifyCommentReply(Long recipientId, Long actorId,
                            Long cardId, String cardTitle);

    // Notifies user when task becomes overdue (optionally triggers email)
    void notifyOverdue(Long recipientId, Long cardId,
                       String cardTitle, String dueDate,
                       String recipientEmail);
}