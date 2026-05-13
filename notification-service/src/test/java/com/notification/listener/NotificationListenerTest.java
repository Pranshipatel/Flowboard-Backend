package com.notification.listener;

import com.notification.dto.SendNotificationRequest;
import com.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationListenerTest {

    @Mock
    private NotificationService notificationService;

    private NotificationListener listener;

    @BeforeEach
    void setUp() {
        listener = new NotificationListener(notificationService);
    }

    @Test
    void handleNotificationMessage_ShouldSendNotification() {
        SendNotificationRequest request = new SendNotificationRequest();
        request.setRecipientId(2L);

        listener.handleNotificationMessage(request);

        verify(notificationService).send(request);
    }

    @Test
    void handleNotificationMessage_WhenServiceFails_ShouldSwallowException() {
        SendNotificationRequest request = new SendNotificationRequest();
        request.setRecipientId(2L);
        doThrow(new RuntimeException("down")).when(notificationService).send(request);

        listener.handleNotificationMessage(request);

        verify(notificationService).send(request);
    }
}
