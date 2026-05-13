package com.notification.service;

import com.notification.dto.NotificationResponse;
import com.notification.dto.SendBulkNotificationRequest;
import com.notification.dto.SendNotificationRequest;
import com.notification.entity.Notification;
import com.notification.entity.NotificationType;
import com.notification.exception.CustomException;
import com.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private EmailNotificationService emailService;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private Notification testNotification;
    private SendNotificationRequest sendRequest;

    @BeforeEach
    void setUp() {
        testNotification = Notification.builder()
                .id(1L)
                .recipientId(2L)
                .actorId(1L)
                .type(NotificationType.ASSIGNMENT)
                .title("Test Title")
                .message("Test Message")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        sendRequest = new SendNotificationRequest();
        sendRequest.setRecipientId(2L);
        sendRequest.setActorId(1L);
        sendRequest.setType(NotificationType.ASSIGNMENT);
        sendRequest.setTitle("Test Title");
        sendRequest.setMessage("Test Message");
    }

    @Test
    void send_WhenValid_ShouldSaveAndReturn() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> {
            Notification n = i.getArgument(0);
            n.setId(1L);
            return n;
        });

        NotificationResponse response = notificationService.send(sendRequest);

        assertNotNull(response);
        assertEquals("Test Title", response.getTitle());
        verify(notificationRepository).save(any(Notification.class));
        verifyNoInteractions(emailService);
    }

    @Test
    void send_WhenEmailRequested_ShouldSendEmail() {
        sendRequest.setSendEmail(true);
        sendRequest.setRecipientEmail("test@test.com");

        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> {
            Notification n = i.getArgument(0);
            n.setId(1L);
            return n;
        });

        NotificationResponse response = notificationService.send(sendRequest);

        assertNotNull(response);
        verify(notificationRepository).save(any(Notification.class));
        verify(emailService).sendNotificationEmail(eq("test@test.com"), eq("Test Title"), eq("Test Message"), any());
    }

    @Test
    void markAsRead_WhenExists_ShouldUpdate() {
        when(notificationRepository.existsByIdAndRecipientId(1L, 2L)).thenReturn(true);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));

        NotificationResponse response = notificationService.markAsRead(1L, 2L);

        assertTrue(response.isRead());
        verify(notificationRepository).save(testNotification);
    }

    @Test
    void markAsRead_WhenNotRecipient_ShouldThrowException() {
        when(notificationRepository.existsByIdAndRecipientId(1L, 3L)).thenReturn(false);

        assertThrows(CustomException.class, () -> notificationService.markAsRead(1L, 3L));
    }

    @Test
    void delete_WhenRecipient_ShouldDelete() {
        when(notificationRepository.existsByIdAndRecipientId(1L, 2L)).thenReturn(true);

        notificationService.deleteNotification(1L, 2L);

        verify(notificationRepository).deleteById(1L);
    }

    @Test
    void sendBulk_WhenValid_ShouldSaveAll() {
        SendBulkNotificationRequest bulkRequest = new SendBulkNotificationRequest();
        bulkRequest.setRecipientIds(List.of(2L, 3L));
        bulkRequest.setActorId(1L);
        bulkRequest.setType(NotificationType.ASSIGNMENT);
        bulkRequest.setTitle("Bulk Title");
        bulkRequest.setMessage("Bulk Message");
        bulkRequest.setRelatedId(9L);
        bulkRequest.setRelatedType("CARD");
        bulkRequest.setDeepLinkUrl("/cards/9");

        List<NotificationResponse> responses = notificationService.sendBulk(bulkRequest);

        assertEquals(2, responses.size());
        verify(notificationRepository).saveAll(anyList());
    }

    @Test
    void queryAndCleanupMethods_ShouldDelegateToRepository() {
        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(2L)).thenReturn(List.of(testNotification));
        when(notificationRepository.findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(2L)).thenReturn(List.of(testNotification));
        when(notificationRepository.findByRecipientIdAndTypeOrderByCreatedAtDesc(2L, NotificationType.ASSIGNMENT)).thenReturn(List.of(testNotification));
        when(notificationRepository.findAll()).thenReturn(List.of(testNotification));
        when(notificationRepository.countByRecipientIdAndIsReadFalse(2L)).thenReturn(3L);

        assertEquals(1, notificationService.getByRecipient(2L).size());
        assertEquals(1, notificationService.getUnreadByRecipient(2L).size());
        assertEquals(1, notificationService.getByRecipientAndType(2L, NotificationType.ASSIGNMENT).size());
        assertEquals(1, notificationService.getAll().size());
        assertEquals(3L, notificationService.getUnreadCount(2L));

        notificationService.markAllAsRead(2L);
        notificationService.deleteReadNotifications(2L);

        verify(notificationRepository).markAllAsRead(2L);
        verify(notificationRepository).deleteReadByRecipientId(2L);
    }

    @Test
    void markAsRead_WhenMissingAfterOwnershipCheck_ShouldThrowException() {
        when(notificationRepository.existsByIdAndRecipientId(1L, 2L)).thenReturn(true);
        when(notificationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(CustomException.class, () -> notificationService.markAsRead(1L, 2L));
    }

    @Test
    void delete_WhenNotRecipient_ShouldThrowException() {
        when(notificationRepository.existsByIdAndRecipientId(1L, 3L)).thenReturn(false);

        assertThrows(CustomException.class, () -> notificationService.deleteNotification(1L, 3L));
    }

    @Test
    void notifyDueDateApproaching_ShouldBuildAndSendNotification() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> {
            Notification n = i.getArgument(0);
            n.setId(10L);
            return n;
        });

        notificationService.notifyDueDateApproaching(2L, 99L, "Important card", "2 hours");

        verify(notificationRepository).save(argThat(notification ->
                notification.getRecipientId().equals(2L)
                        && notification.getRelatedId().equals(99L)
                        && notification.getType() == NotificationType.DUE_DATE
                        && notification.getDeepLinkUrl().equals("/cards/99")
        ));
    }
}
