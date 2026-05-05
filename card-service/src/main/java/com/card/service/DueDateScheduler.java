package com.card.service;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.card.client.dto.SendNotificationRequest;
import com.card.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.card.entity.Card;
import com.card.entity.CardStatus;
import com.card.repository.CardRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DueDateScheduler {

    private final CardRepository cardRepository;
    private final RabbitTemplate rabbitTemplate;

    @Scheduled(cron = "0 0 8 * * *")
    public void notifyDueTomorrow() {

        LocalDate tomorrow = LocalDate.now().plusDays(10);

        List<Card> cards = cardRepository
                .findByDueDateAndIsArchivedFalseAndStatusNot(
                        tomorrow,
                        CardStatus.DONE
                );

        log.info(
                "[Schedular] Due-tomorrow check: {} cards due on {}",
                cards.size(),
                tomorrow
        );

        for (Card card : cards) {

            if (card.getAssigneeId() == null) continue;

            try {
                SendNotificationRequest notification = SendNotificationRequest.builder()
                        .recipientId(card.getAssigneeId())
                        .type("DUE_DATE")
                        .title("Due Date Approaching: " + card.getTitle())
                        .message("This card is due in 24 hours.")
                        .relatedId(card.getId())
                        .relatedType("CARD")
                        .build();

                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.NOTIFICATION_EXCHANGE,
                        RabbitMQConfig.NOTIFICATION_ROUTING_KEY,
                        notification
                );

                log.debug(
                        "Due-tomorrow notification sent for cardId={}",
                        card.getId()
                );

            } catch (Exception e) {
                log.error(
                        "Failed to notify due-date for cardId={}: {}",
                        card.getId(),
                        e.getMessage()
                );
            }
        }
    }

    @Scheduled(cron = "0 0 * * * *")
    public void notifyDueInTwoHours() {

        LocalDate today = LocalDate.now();
        int currentHour = LocalDateTime.now().getHour();

        if (currentHour != 22) return;

        List<Card> cards = cardRepository
                .findByDueDateAndIsArchivedFalseAndStatusNot(
                        today,
                        CardStatus.DONE
                );

        log.info(
                "[Schedular] Due-today urgent check: {} cards",
                cards.size()
        );

        for (Card card : cards) {

            if (card.getAssigneeId() == null) continue;

            try {
                SendNotificationRequest notification = SendNotificationRequest.builder()
                        .recipientId(card.getAssigneeId())
                        .type("DUE_DATE")
                        .title("Urgent: " + card.getTitle())
                        .message("This card is due in 2 hours.")
                        .relatedId(card.getId())
                        .relatedType("CARD")
                        .build();

                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.NOTIFICATION_EXCHANGE,
                        RabbitMQConfig.NOTIFICATION_ROUTING_KEY,
                        notification
                );

            } catch (Exception e) {
                log.error(
                        "Failed urgent-due-date notification cardId={}: {}",
                        card.getId(),
                        e.getMessage()
                );
            }
        }
    }


}