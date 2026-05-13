package com.card.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.card.config.RabbitMQConfig;
import com.card.entity.Card;
import com.card.entity.CardStatus;
import com.card.repository.CardRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
class DueDateSchedulerTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private DueDateScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new DueDateScheduler(cardRepository, rabbitTemplate);
    }

    @Test
    void notifyDueTomorrowSendsOnlyAssignedCardsAndContinuesOnFailures() {
        Card assigned = Card.builder()
                .id(1L)
                .title("Assigned")
                .assigneeId(7L)
                .status(CardStatus.TO_DO)
                .build();
        Card unassigned = Card.builder()
                .id(2L)
                .title("Unassigned")
                .status(CardStatus.TO_DO)
                .build();
        when(cardRepository.findByDueDateAndIsArchivedFalseAndStatusNot(
                eq(LocalDate.now().plusDays(1)), eq(CardStatus.DONE)))
                .thenReturn(List.of(assigned, unassigned));
        doThrow(new RuntimeException("broker down")).when(rabbitTemplate)
                .convertAndSend(any(String.class), any(String.class), any(Object.class));

        scheduler.notifyDueTomorrow();

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.NOTIFICATION_EXCHANGE),
                eq(RabbitMQConfig.NOTIFICATION_ROUTING_KEY),
                any(Object.class));
    }

    @Test
    void notifyDueInTwoHoursReturnsOutsideScheduledHour() {
        scheduler.notifyDueInTwoHours();

        verify(rabbitTemplate, never()).convertAndSend(any(String.class), any(String.class), any(Object.class));
    }
}
