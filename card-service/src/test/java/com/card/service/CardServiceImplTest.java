package com.card.service;

import com.card.dto.CardResponse;
import com.card.dto.CreateCardRequest;
import com.card.dto.AssignCardRequest;
import com.card.dto.MoveCardRequest;
import com.card.dto.ReorderCardRequest;
import com.card.dto.SetPriorityRequest;
import com.card.dto.SetStatusRequest;
import com.card.dto.UpdateCardRequest;
import com.card.entity.CardActivity;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

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
                .createdAt(LocalDateTime.now())
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
    void createCard_WithPosition_ShouldShiftAndUseRequestedPosition() {
        createRequest.setPosition(2);
        createRequest.setPriority(Priority.HIGH);
        createRequest.setAssigneeId(9L);
        when(cardRepository.countByBoardIdAndIsArchivedFalse(100L)).thenReturn(1L);
        when(cardRepository.save(any(Card.class))).thenAnswer(i -> {
            Card c = i.getArgument(0);
            c.setId(2L);
            return c;
        });

        CardResponse response = cardService.createCard(createRequest, 1L, false);

        assertEquals(2, response.getPosition());
        assertEquals(Priority.HIGH, response.getPriority());
        verify(cardRepository).shiftPositionsRight(10L, 2);
        verify(activityRepository, times(2)).save(any());
    }

    @Test
    void createCard_WhenFreeLimitReached_ShouldThrowException() {
        when(cardRepository.countByBoardIdAndIsArchivedFalse(100L)).thenReturn(2L);

        assertThrows(CustomException.class, () -> cardService.createCard(createRequest, 1L, false));
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
    void listBoardAssigneeAndFilterQueriesReturnResponses() {
        when(cardRepository.findByListIdAndIsArchivedFalseOrderByPosition(10L)).thenReturn(List.of(testCard));
        when(cardRepository.findByBoardIdAndIsArchivedFalse(100L)).thenReturn(List.of(testCard));
        when(cardRepository.findByAssigneeIdAndIsArchivedFalse(7L)).thenReturn(List.of(testCard));
        when(cardRepository.findByBoardIdAndStatusAndIsArchivedFalse(100L, CardStatus.TO_DO)).thenReturn(List.of(testCard));
        when(cardRepository.findByBoardIdAndPriorityAndIsArchivedFalse(100L, Priority.MEDIUM)).thenReturn(List.of(testCard));

        assertEquals(1, cardService.getCardByList(10L).size());
        assertEquals(1, cardService.getCardByBoard(100L).size());
        assertEquals(1, cardService.getCardByAssignee(7L).size());
        assertEquals(1, cardService.getCardsByStatus(100L, CardStatus.TO_DO).size());
        assertEquals(1, cardService.getCardsByPriority(100L, Priority.MEDIUM).size());
    }

    @Test
    void updateCard_WhenValid_ShouldUpdateFieldsAndLogChanges() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        UpdateCardRequest request = new UpdateCardRequest();
        request.setTitle("Updated");
        request.setDescription("New description");
        request.setPriority(Priority.HIGH);
        request.setStatus(CardStatus.IN_PROGRESS);
        request.setDueDate(LocalDate.now().plusDays(1));
        request.setStartDate(LocalDate.now());
        request.setCoverColor("#fff");

        CardResponse response = cardService.updateCard(1L, request, 77L);

        assertEquals("Updated", response.getTitle());
        assertEquals(CardStatus.IN_PROGRESS, response.getStatus());
        assertEquals(Priority.HIGH, response.getPriority());
        verify(activityRepository, times(3)).save(any());
        verify(cardRepository).save(testCard);
    }

    @Test
    void updateCard_WhenOptionalFieldsUnchanged_ShouldOnlyUpdateTitle() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        UpdateCardRequest request = new UpdateCardRequest();
        request.setTitle("Title Only");

        CardResponse response = cardService.updateCard(1L, request, 77L);

        assertEquals("Title Only", response.getTitle());
        assertEquals("Test Description", response.getDescription());
        assertEquals(Priority.MEDIUM, response.getPriority());
        assertEquals(CardStatus.TO_DO, response.getStatus());
        verify(activityRepository, never()).save(any());
        verify(cardRepository).save(testCard);
    }

    @Test
    void updateCard_WhenArchived_ShouldThrowException() {
        testCard.setArchived(true);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        UpdateCardRequest request = new UpdateCardRequest();
        request.setTitle("Updated");

        assertThrows(CustomException.class, () -> cardService.updateCard(1L, request, 77L));
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

    @Test
    void deleteCard_WhenArchived_ShouldNotShiftPositions() {
        testCard.setArchived(true);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));

        cardService.deleteCard(1L, 1L);

        verify(cardRepository, never()).shiftPositionsLeft(anyLong(), anyInt());
        verify(cardRepository).delete(testCard);
    }

    @Test
    void moveCard_WithPosition_ShouldMoveAndLog() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        MoveCardRequest request = new MoveCardRequest();
        request.setTargetListId(20L);
        request.setTargetBoardId(200L);
        request.setTargetPosition(3);

        CardResponse response = cardService.moveCard(1L, request, 9L);

        assertEquals(20L, response.getListId());
        assertEquals(200L, response.getBoardId());
        assertEquals(3, response.getPosition());
        verify(cardRepository).shiftPositionsLeft(10L, 0);
        verify(cardRepository).shiftPositionsRight(20L, 3);
        verify(activityRepository).save(any());
    }

    @Test
    void moveCard_WithoutPosition_ShouldAppendToTargetList() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        when(cardRepository.findMaxPositionByListId(20L)).thenReturn(Optional.of(4));
        MoveCardRequest request = new MoveCardRequest();
        request.setTargetListId(20L);
        request.setTargetBoardId(200L);

        CardResponse response = cardService.moveCard(1L, request, 9L);

        assertEquals(5, response.getPosition());
    }

    @Test
    void reorderCards_UpdatesPositionsAndRejectsForeignCards() {
        Card second = Card.builder()
                .id(2L)
                .listId(10L)
                .boardId(100L)
                .title("Second")
                .position(1)
                .priority(Priority.LOW)
                .status(CardStatus.TO_DO)
                .createdById(1L)
                .build();
        when(cardRepository.findByListIdAndIsArchivedFalseOrderByPosition(10L))
                .thenReturn(List.of(testCard, second), List.of(second, testCard));

        ReorderCardRequest request = new ReorderCardRequest();
        request.setListId(10L);
        request.setOrderedCardIds(List.of(2L, 1L));

        assertEquals(2, cardService.reorderCards(request, 9L).size());
        assertEquals(0, second.getPosition());
        assertEquals(1, testCard.getPosition());

        ReorderCardRequest invalid = new ReorderCardRequest();
        invalid.setListId(10L);
        invalid.setOrderedCardIds(List.of(99L));
        when(cardRepository.findByListIdAndIsArchivedFalseOrderByPosition(10L)).thenReturn(List.of(testCard));
        assertThrows(CustomException.class, () -> cardService.reorderCards(invalid, 9L));
    }

    @Test
    void archiveAndUnarchiveValidateCurrentState() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));

        assertTrue(cardService.archiveCard(1L, 1L).isArchived());

        when(cardRepository.findMaxPositionByListId(10L)).thenReturn(Optional.of(8));
        assertFalse(cardService.unarchiveCard(1L, 1L).isArchived());
        assertEquals(9, testCard.getPosition());

        assertThrows(CustomException.class, () -> cardService.unarchiveCard(1L, 1L));
    }

    @Test
    void unarchive_WhenCardIsNotArchived_ShouldThrowException() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));

        assertThrows(CustomException.class, () -> cardService.unarchiveCard(1L, 1L));
    }

    @Test
    void archivedQueriesReturnResponses() {
        when(cardRepository.findByBoardIdAndIsArchivedTrue(100L)).thenReturn(List.of(testCard));
        when(cardRepository.findByListIdAndIsArchivedTrue(10L)).thenReturn(List.of(testCard));

        assertEquals(1, cardService.getArchivedCardsByBoard(100L).size());
        assertEquals(1, cardService.getArchivedCardsByList(10L).size());
    }

    @Test
    void assignmentPriorityAndStatusOperationsUpdateCard() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));

        AssignCardRequest assign = new AssignCardRequest();
        assign.setAssigneeId(44L);
        assertEquals(44L, cardService.setAssignee(1L, assign, 1L).getAssigneeId());

        SetPriorityRequest priority = new SetPriorityRequest();
        priority.setPriority(Priority.HIGH);
        assertEquals(Priority.HIGH, cardService.setPriority(1L, priority, 1L).getPriority());

        SetStatusRequest status = new SetStatusRequest();
        status.setStatus(CardStatus.DONE);
        assertEquals(CardStatus.DONE, cardService.setStatus(1L, status, 1L).getStatus());
    }

    @Test
    void setAssignee_WhenRequestIsNull_ShouldUnassignCard() {
        testCard.setAssigneeId(44L);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        AssignCardRequest assign = new AssignCardRequest();

        CardResponse response = cardService.setAssignee(1L, assign, 1L);

        assertNull(response.getAssigneeId());
        verify(activityRepository).save(argThat(activity ->
                "ASSIGNMENT".equals(activity.getActionType())
                        && "44".equals(activity.getOldValue())
                        && "none".equals(activity.getNewValue())
        ));
    }

    @Test
    void overdueAndSearchQueriesReturnResponses() {
        testCard.setDueDate(LocalDate.now().minusDays(1));
        when(cardRepository.findOverdueByBoardId(eq(100L), any(LocalDate.class))).thenReturn(List.of(testCard));
        when(cardRepository.findAllOverdue(any(LocalDate.class))).thenReturn(List.of(testCard));
        when(cardRepository.searchByTitle(100L, "test")).thenReturn(List.of(testCard));
        when(cardRepository.searchByTitleOrAssignee("test", 1L)).thenReturn(List.of(testCard));

        assertTrue(cardService.getOverdueCardsByBoard(100L).get(0).isOverdue());
        assertEquals(1, cardService.getAllOverdueCards().size());
        assertEquals(1, cardService.searchCards(100L, "test").size());
        assertEquals(1, cardService.searchByTitleOrAssignee("test", 1L).size());
    }

    @Test
    void getCardActivityMapsActivityResponses() {
        CardActivity activity = CardActivity.builder()
                .id(5L)
                .cardId(1L)
                .actorId(9L)
                .actionType("CREATE")
                .description("created")
                .oldValue(null)
                .newValue("Test Card")
                .createdAt(LocalDateTime.now())
                .build();
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        when(activityRepository.findByCardIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(activity));

        assertEquals("CREATE", cardService.getCardActivity(1L).get(0).getActionType());
    }

    @Test
    void getBoardStatsHandlesEmptyAndMapsRows() {
        assertTrue(cardService.getBoardStats(null).isEmpty());
        assertTrue(cardService.getBoardStats(List.of()).isEmpty());
        List<Object[]> rows = Collections.singletonList(new Object[] {100L, 3L, 1L});
        when(cardRepository.getStatsByBoardIds(List.of(100L))).thenReturn(rows);

        var stats = cardService.getBoardStats(List.of(100L));

        assertEquals(100L, stats.get(0).getBoardId());
        assertEquals(3L, stats.get(0).getTotalCards());
        assertEquals(1L, stats.get(0).getDoneCards());
    }
}
