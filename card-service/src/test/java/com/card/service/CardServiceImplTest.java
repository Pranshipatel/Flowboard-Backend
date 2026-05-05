package com.card.service;

import com.card.dto.CardResponse;
import com.card.dto.CreateCardRequest;
import com.card.entity.Card;
import com.card.entity.Priority;
import com.card.entity.CardStatus;
import com.card.exception.CustomException;
import com.card.repository.CardActivityRepository;
import com.card.repository.CardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceImplTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private CardActivityRepository activityRepository;

    @InjectMocks
    private CardServiceImpl cardService;

    private Card testCard;
    private CreateCardRequest createRequest;

    @BeforeEach
    void setUp() {
        testCard = Card.builder()
                .id(1L)
                .listId(10L)
                .boardId(100L)
                .title("Test Card")
                .description("Test Description")
                .position(0)
                .priority(Priority.MEDIUM)
                .status(CardStatus.TO_DO)
                .isArchived(false)
                .createdById(1L)
                .build();

        createRequest = new CreateCardRequest();
        createRequest.setListId(10L);
        createRequest.setBoardId(100L);
        createRequest.setTitle("Test Card");
        createRequest.setDescription("Test Description");
    }

    @Test
    void createCard_WithNoPosition_ShouldPutAtEnd() {
        when(cardRepository.findMaxPositionByListId(10L)).thenReturn(Optional.of(5));
        when(cardRepository.save(any(Card.class))).thenAnswer(i -> {
            Card c = i.getArgument(0);
            c.setId(1L);
            return c;
        });

        CardResponse response = cardService.createCard(createRequest, 1L, true);

        assertNotNull(response);
        assertEquals("Test Card", response.getTitle());
        assertEquals(6, response.getPosition());
        verify(cardRepository).findMaxPositionByListId(10L);
        verify(cardRepository).save(any(Card.class));
        verify(activityRepository).save(any());
    }

    @Test
    void getCardById_WhenExists_ShouldReturnCard() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));

        CardResponse response = cardService.getCardById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Test Card", response.getTitle());
    }

    @Test
    void getCardById_WhenNotFound_ShouldThrowException() {
        when(cardRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(CustomException.class, () -> cardService.getCardById(1L));
    }

    @Test
    void archiveCard_WhenValid_ShouldArchive() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));

        CardResponse response = cardService.archiveCard(1L, 1L);

        assertTrue(response.isArchived());
        verify(cardRepository).shiftPositionsLeft(10L, 0);
        verify(cardRepository).save(testCard);
    }

    @Test
    void archiveCard_WhenAlreadyArchived_ShouldThrowException() {
        testCard.setArchived(true);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));

        assertThrows(CustomException.class, () -> cardService.archiveCard(1L, 1L));
    }

    @Test
    void deleteCard_WhenValid_ShouldDelete() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));

        cardService.deleteCard(1L, 1L);

        verify(cardRepository).shiftPositionsLeft(10L, 0);
        verify(cardRepository).delete(testCard);
    }
}
