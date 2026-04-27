package com.notification.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.notification.dto.NotificationResponse;
import com.notification.dto.SendBulkNotificationRequest;
import com.notification.dto.SendNotificationRequest;
import com.notification.entity.NotificationType;
import com.notification.exception.CustomException;
import com.notification.service.NotificationService;



@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // Resolves user ID from header (mandatory for user-specific operations)
    private Long resolveUserId(Long userIdHeader){
        if(userIdHeader != null) return userIdHeader;
        throw new CustomException("Missing required header: X-User-Id", HttpStatus.BAD_REQUEST);
    }

    @PostMapping("/send")
    public ResponseEntity<NotificationResponse> send(
            @Valid @RequestBody SendNotificationRequest request){
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificationService.send(request));
    }

    @PostMapping("/send/bulk")
    public ResponseEntity<List<NotificationResponse>> sendBulk(
            @Valid @RequestBody SendBulkNotificationRequest request){
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificationService.sendBulk(request));
    }

    @PostMapping("/notify/assignment")
    public ResponseEntity<String> notifyAssignment(
            @RequestParam Long recipientId,
            @RequestParam Long actorId,
            @RequestParam Long cardId,
            @RequestParam String cardTitle,
            @RequestParam(required = false) String recipientEmail){

        notificationService.notifyAssignment(
                recipientId, actorId, cardId, cardTitle, recipientEmail);

        return ResponseEntity.ok("Assignment notification sent successfully");
    }

    // Handles due-date notification via request body (used by integrations)
    @PostMapping("/notify/due-date-body")
    public ResponseEntity<String> notifyDueDateBody(
            @RequestBody Map<String, Object> req){

        Long recipientId = Long.valueOf(req.get("recipientId").toString());
        Long cardId = Long.valueOf(req.get("cardId").toString());
        String cardTitle = req.get("cardTitle").toString();
        String timeLeft = req.get("timeLeft").toString();

        notificationService.notifyDueDateApproaching(
                recipientId, cardId, cardTitle, timeLeft);

        return ResponseEntity.ok("Due date notification sent successfully");
    }

    @PostMapping("/notify/overdue-body")
    public ResponseEntity<String> notifyOverdueBody(
            @RequestBody Map<String, Object> req){

        Long recipientId = Long.valueOf(req.get("recipientId").toString());
        Long cardId = Long.valueOf(req.get("cardId").toString());
        String cardTitle = req.get("cardTitle").toString();
        String dueDate = req.get("dueDate").toString();

        String email = req.containsKey("recipientEmail") && req.get("recipientEmail") != null
                ? req.get("recipientEmail").toString() : null;

        notificationService.notifyOverdue(
                recipientId, cardId, cardTitle, dueDate, email);

        return ResponseEntity.ok("Overdue notification sent successfully");
    }

    @PostMapping("/notify/mention")
    public ResponseEntity<String> notifyMention(
            @RequestParam Long recipientId,
            @RequestParam Long actorId,
            @RequestParam Long cardId,
            @RequestParam String cardTitle){

        notificationService.notifyMention(recipientId, actorId, cardId, cardTitle);

        return ResponseEntity.ok("Mention notification sent successfully");
    }

    @PostMapping("/notify/due-date")
    public ResponseEntity<String> notifyDueDate(
            @RequestParam Long recipientId,
            @RequestParam Long cardId,
            @RequestParam String cardTitle,
            @RequestParam String timeLeft){

        notificationService.notifyDueDateApproaching(
                recipientId, cardId, cardTitle, timeLeft);

        return ResponseEntity.ok("Due date notification sent successfully");
    }

    @PostMapping("/notify/done")
    public ResponseEntity<String> notifyDone(
            @RequestParam Long recipientId,
            @RequestParam Long actorId,
            @RequestParam Long cardId,
            @RequestParam String cardTitle){

        notificationService.notifyCardMovedToDone(
                recipientId, actorId, cardId, cardTitle);

        return ResponseEntity.ok("Completion notification sent successfully");
    }

    @PostMapping("/notify/reply")
    public ResponseEntity<String> notifyReply(
            @RequestParam Long recipientId,
            @RequestParam Long actorId,
            @RequestParam Long cardId,
            @RequestParam String cardTitle){

        notificationService.notifyCommentReply(
                recipientId, actorId, cardId, cardTitle);

        return ResponseEntity.ok("Reply notification sent successfully");
    }

    @PostMapping("/notify/overdue")
    public ResponseEntity<String> notifyOverdue(
            @RequestParam Long recipientId,
            @RequestParam Long cardId,
            @RequestParam String cardTitle,
            @RequestParam String dueDate,
            @RequestParam(required = false) String recipientEmail){

        notificationService.notifyOverdue(
                recipientId, cardId, cardTitle, dueDate, recipientEmail);

        return ResponseEntity.ok("Overdue notification sent successfully");
    }

    // ================= RETRIEVAL =================

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(
            @RequestHeader(value = "X-User-Id", required = false) Long userId){

        return ResponseEntity.ok(
                notificationService.getByRecipient(resolveUserId(userId)));
    }

    @GetMapping("/unread")
    public ResponseEntity<List<NotificationResponse>> getUnread(
            @RequestHeader(value = "X-User-Id", required = false) Long userId){

        return ResponseEntity.ok(
                notificationService.getUnreadByRecipient(resolveUserId(userId)));
    }

    @GetMapping("/unread/count")
    public ResponseEntity<Long> getUnreadCount(
            @RequestHeader(value = "X-User-Id", required = false) Long userId){

        return ResponseEntity.ok(
                notificationService.getUnreadCount(resolveUserId(userId)));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<NotificationResponse>> getByType(
            @PathVariable NotificationType type,
            @RequestHeader(value = "X-User-Id", required = false) Long userId){

        return ResponseEntity.ok(
                notificationService.getByRecipientAndType(
                        resolveUserId(userId), type));
    }

    // Admin-only endpoint
    @GetMapping("/all")
    public ResponseEntity<List<NotificationResponse>> getAll(
            @RequestHeader(value = "X-User-Role", required = false) String role){

        if (!"PLATFORM_ADMIN".equals(role)) {
            throw new CustomException(
                    "Access denied: administrative privileges required",
                    HttpStatus.FORBIDDEN
            );
        }

        return ResponseEntity.ok(notificationService.getAll());
    }

    // ================= READ STATE =================

    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId){

        return ResponseEntity.ok(
                notificationService.markAsRead(id, resolveUserId(userId)));
    }

    @PutMapping("/read/all")
    public ResponseEntity<String> markAllAsRead(
            @RequestHeader(value = "X-User-Id", required = false) Long userId){

        notificationService.markAllAsRead(resolveUserId(userId));

        return ResponseEntity.ok("All notifications marked as read successfully");
    }

    // ================= CLEANUP =================

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId){

        notificationService.deleteNotification(id, resolveUserId(userId));

        return ResponseEntity.ok("Notification deleted successfully");
    }

    @DeleteMapping("/read/all")
    public ResponseEntity<String> deleteRead(
            @RequestHeader(value = "X-User-Id", required = false) Long userId){

        notificationService.deleteReadNotifications(resolveUserId(userId));

        return ResponseEntity.ok("Read notifications deleted successfully");
    }
}