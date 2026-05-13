package com.notification.controller;

import com.notification.dto.NotificationResponse;
import com.notification.dto.SendBulkNotificationRequest;
import com.notification.dto.SendNotificationRequest;
import com.notification.entity.NotificationType;
import com.notification.exception.CustomException;
import com.notification.service.NotificationService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    private NotificationController controller;
    private NotificationResponse response;

    @BeforeEach
    void setUp() {
        controller = new NotificationController(notificationService);
        response = NotificationResponse.builder()
                .id(1L)
                .recipientId(2L)
                .type(NotificationType.ASSIGNMENT)
                .title("Title")
                .build();
    }

    @Test
    void endpoints_ShouldDelegateToServiceAndReturnResponses() {
        SendNotificationRequest send = new SendNotificationRequest();
        SendBulkNotificationRequest bulk = new SendBulkNotificationRequest();
        Map<String, Object> dueDate = Map.of(
                "recipientId", 2L,
                "cardId", 10L,
                "cardTitle", "Card",
                "timeLeft", "2 hours"
        );

        when(notificationService.send(send)).thenReturn(response);
        when(notificationService.sendBulk(bulk)).thenReturn(List.of(response));
        when(notificationService.getByRecipient(2L)).thenReturn(List.of(response));
        when(notificationService.getUnreadByRecipient(2L)).thenReturn(List.of(response));
        when(notificationService.getUnreadCount(2L)).thenReturn(5L);
        when(notificationService.getByRecipientAndType(2L, NotificationType.ASSIGNMENT)).thenReturn(List.of(response));
        when(notificationService.getAll()).thenReturn(List.of(response));
        when(notificationService.markAsRead(1L, 2L)).thenReturn(response);

        assertEquals(HttpStatus.CREATED, controller.send(send).getStatusCode());
        assertEquals(1, controller.sendBulk(bulk).getBody().size());
        assertEquals("Due date notification sent successfully", controller.notifyDueDateBody(dueDate).getBody());
        assertEquals(1, controller.getMyNotifications(2L).getBody().size());
        assertEquals(1, controller.getUnread(2L).getBody().size());
        assertEquals(5L, controller.getUnreadCount(2L).getBody());
        assertEquals(1, controller.getByType(NotificationType.ASSIGNMENT, 2L).getBody().size());
        assertEquals(1, controller.getAll("PLATFORM_ADMIN").getBody().size());
        assertEquals(response, controller.markAsRead(1L, 2L).getBody());
        assertEquals("All notifications marked as read successfully", controller.markAllAsRead(2L).getBody());
        assertEquals("Notification deleted successfully", controller.delete(1L, 2L).getBody());
        assertEquals("Read notifications deleted successfully", controller.deleteRead(2L).getBody());

        verify(notificationService).notifyDueDateApproaching(2L, 10L, "Card", "2 hours");
        verify(notificationService).markAllAsRead(2L);
        verify(notificationService).deleteNotification(1L, 2L);
        verify(notificationService).deleteReadNotifications(2L);
    }

    @Test
    void endpoints_WhenUserHeaderOrAdminRoleMissing_ShouldThrow() {
        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(CustomException.class, () -> controller.getUnread(null)).getStatus());
        assertEquals(HttpStatus.FORBIDDEN,
                assertThrows(CustomException.class, () -> controller.getAll("USER")).getStatus());
    }
}
