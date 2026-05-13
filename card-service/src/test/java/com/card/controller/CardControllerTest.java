package com.card.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.card.dto.AssignCardRequest;
import com.card.dto.BoardStatsResponse;
import com.card.dto.CardActivityResponse;
import com.card.dto.CardResponse;
import com.card.dto.CreateCardRequest;
import com.card.dto.MoveCardRequest;
import com.card.dto.ReorderCardRequest;
import com.card.dto.SetPriorityRequest;
import com.card.dto.SetStatusRequest;
import com.card.dto.UpdateCardRequest;
import com.card.entity.CardStatus;
import com.card.entity.Priority;
import com.card.exception.CustomException;
import com.card.service.CardService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class CardControllerTest {

    @Mock
    private CardService cardService;

    private CardController controller;
    private CardResponse card;

    @BeforeEach
    void setUp() {
        controller = new CardController(cardService);
        card = CardResponse.builder()
                .id(1L)
                .listId(10L)
                .boardId(100L)
                .title("Card")
                .position(0)
                .priority(Priority.MEDIUM)
                .status(CardStatus.TO_DO)
                .build();
    }

    @Test
    void createResolvesUserAndPremiumHeaders() {
        CreateCardRequest request = new CreateCardRequest();
        when(cardService.createCard(eq(request), eq(5L), eq(true))).thenReturn(card);

        var response = controller.create(request, 5L, "PREMIUM", "ACTIVE");

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(card, response.getBody());
    }

    @Test
    void createWithoutUserIdThrows() {
        assertThrows(CustomException.class, () -> controller.create(new CreateCardRequest(), null, "FREE", "EXPIRED"));
    }

    @Test
    void readEndpointsDelegateAndLimitFreeUsers() {
        when(cardService.getCardById(1L)).thenReturn(card);
        when(cardService.getCardByList(10L)).thenReturn(List.of(card, card, card));
        when(cardService.getCardByBoard(100L)).thenReturn(List.of(card, card, card));
        when(cardService.getCardByAssignee(7L)).thenReturn(List.of(card));

        assertEquals(card, controller.getById(1L).getBody());
        assertEquals(2, controller.getByList(10L, "FREE", "EXPIRED").getBody().size());
        assertEquals(3, controller.getByBoard(100L, "PREMIUM", "ACTIVE").getBody().size());
        assertEquals(1, controller.getByAssignee(7L).getBody().size());
    }

    @Test
    void mutationEndpointsDelegateWithUserIdHeader() {
        UpdateCardRequest update = new UpdateCardRequest();
        MoveCardRequest move = new MoveCardRequest();
        ReorderCardRequest reorder = new ReorderCardRequest();
        AssignCardRequest assign = new AssignCardRequest();
        SetPriorityRequest priority = new SetPriorityRequest();
        SetStatusRequest status = new SetStatusRequest();

        when(cardService.updateCard(1L, update, 9L)).thenReturn(card);
        when(cardService.moveCard(1L, move, 9L)).thenReturn(card);
        when(cardService.reorderCards(reorder, 9L)).thenReturn(List.of(card));
        when(cardService.archiveCard(1L, 9L)).thenReturn(card);
        when(cardService.unarchiveCard(1L, 9L)).thenReturn(card);
        when(cardService.setAssignee(1L, assign, 9L)).thenReturn(card);
        when(cardService.setPriority(1L, priority, 9L)).thenReturn(card);
        when(cardService.setStatus(1L, status, 9L)).thenReturn(card);

        assertEquals(card, controller.update(1L, update, 9L).getBody());
        assertEquals("Card deleted successfully", controller.delete(1L, 9L).getBody());
        assertEquals(card, controller.move(1L, move, 9L).getBody());
        assertEquals(1, controller.reorder(reorder, 9L).getBody().size());
        assertEquals(card, controller.archive(1L, 9L).getBody());
        assertEquals(card, controller.unarchive(1L, 9L).getBody());
        assertEquals(card, controller.setAssignment(1L, assign, 9L).getBody());
        assertEquals(card, controller.setPriority(1L, priority, 9L).getBody());
        assertEquals(card, controller.setStatus(1L, status, 9L).getBody());
        verify(cardService).deleteCard(1L, 9L);
    }

    @Test
    void collectionEndpointsDelegate() {
        when(cardService.getArchivedCardsByBoard(100L)).thenReturn(List.of(card));
        when(cardService.getArchivedCardsByList(10L)).thenReturn(List.of(card));
        when(cardService.getCardsByStatus(100L, CardStatus.TO_DO)).thenReturn(List.of(card));
        when(cardService.getCardsByPriority(100L, Priority.HIGH)).thenReturn(List.of(card));
        when(cardService.getOverdueCardsByBoard(100L)).thenReturn(List.of(card));
        when(cardService.getAllOverdueCards()).thenReturn(List.of(card));
        when(cardService.searchCards(100L, "card")).thenReturn(List.of(card));
        when(cardService.searchByTitleOrAssignee("card", 7L)).thenReturn(List.of(card));

        assertEquals(1, controller.getArchivedByBoard(100L).getBody().size());
        assertEquals(1, controller.getArchivedByList(10L).getBody().size());
        assertEquals(1, controller.getByStatus(100L, CardStatus.TO_DO).getBody().size());
        assertEquals(1, controller.getByPriority(100L, Priority.HIGH).getBody().size());
        assertEquals(1, controller.getOverdueByBoard(100L).getBody().size());
        assertEquals(1, controller.getAllOverdue().getBody().size());
        assertEquals(1, controller.search(100L, "card").getBody().size());
        assertEquals(1, controller.searchGlobal("card", 7L).getBody().size());
    }

    @Test
    void activityAndStatsEndpointsDelegate() {
        CardActivityResponse activity = CardActivityResponse.builder().id(1L).build();
        BoardStatsResponse stats = BoardStatsResponse.builder().boardId(100L).totalCards(2).doneCards(1).build();
        when(cardService.getCardActivity(1L)).thenReturn(List.of(activity));
        when(cardService.getBoardStats(List.of(100L))).thenReturn(List.of(stats));

        assertEquals(1, controller.getActivity(1L).getBody().size());
        assertEquals(1, controller.getBoardStats(List.of(100L)).getBody().size());
    }
}
