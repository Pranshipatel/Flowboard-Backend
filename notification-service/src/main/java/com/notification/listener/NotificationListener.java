package com.notification.listener;

import com.notification.config.RabbitMQConfig;
import com.notification.dto.SendNotificationRequest;
import com.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void handleNotificationMessage(SendNotificationRequest request) {
        log.info("Received notification message from queue for recipientId: {}", request.getRecipientId());
        try {
            notificationService.send(request);
        } catch (Exception e) {
            log.error("Failed to process notification message", e);
        }
    }
}
