package com.card.controller;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.card.dto.AssignCardRequest;
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

@RestController
@RequestMapping("/api/v1/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    // Ensure userId is always provided
    private Long resolveUserId(Long userIdHeader) {
        if (userIdHeader != null) return userIdHeader;

        throw new CustomException(
                "User ID header (X-User-Id) must be provided",
                HttpStatus.BAD_REQUEST
        );
    }

    private boolean isPremium(String plan, String status) {
        return "PREMIUM".equalsIgnoreCase(plan) && "ACTIVE".equalsIgnoreCase(status);
    }

    @PostMapping
    public ResponseEntity<CardResponse> create(
            @Valid @RequestBody CreateCardRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Subscription-Plan", required = false, defaultValue = "FREE") String plan,
            @RequestHeader(value = "X-Subscription-Status", required = false, defaultValue = "EXPIRED") String status
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cardService.createCard(request, resolveUserId(userId), isPremium(plan, status)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CardResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(cardService.getCardById(id));
    }

    @GetMapping("/list/{listId}")
    public ResponseEntity<List<CardResponse>> getByList(
            @PathVariable Long listId,
            @RequestHeader(value = "X-Subscription-Plan", required = false, defaultValue = "FREE") String plan,
            @RequestHeader(value = "X-Subscription-Status", required = false, defaultValue = "EXPIRED") String status
    ) {
        List<CardResponse> cards = cardService.getCardByList(listId);
        if (!isPremium(plan, status)) {
            cards = cards.stream().limit(2).toList();
        }
        return ResponseEntity.ok(cards);
    }

    @GetMapping("/board/{boardId}")
    public ResponseEntity<List<CardResponse>> getByBoard(
            @PathVariable Long boardId,
            @RequestHeader(value = "X-Subscription-Plan", required = false, defaultValue = "FREE") String plan,
            @RequestHeader(value = "X-Subscription-Status", required = false, defaultValue = "EXPIRED") String status
    ) {
        List<CardResponse> cards = cardService.getCardByBoard(boardId);
        if (!isPremium(plan, status)) {
            cards = cards.stream().limit(2).toList();
        }
        return ResponseEntity.ok(cards);
    }

    @GetMapping("/assignee/{userId}")
    public ResponseEntity<List<CardResponse>> getByAssignee(
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(cardService.getCardByAssignee(userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CardResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCardRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        return ResponseEntity.ok(
                cardService.updateCard(id, request, resolveUserId(userId))
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        cardService.deleteCard(id, resolveUserId(userId));

        return ResponseEntity.ok("Card deleted successfully");
    }

    @PutMapping("/{id}/move")
    public ResponseEntity<CardResponse> move(
            @PathVariable Long id,
            @Valid @RequestBody MoveCardRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        return ResponseEntity.ok(
                cardService.moveCard(id, request, resolveUserId(userId))
        );
    }

    @PutMapping("/reorder")
    public ResponseEntity<List<CardResponse>> reorder(
            @Valid @RequestBody ReorderCardRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        return ResponseEntity.ok(
                cardService.reorderCards(request, resolveUserId(userId))
        );
    }

    @PutMapping("/{id}/archive")
    public ResponseEntity<CardResponse> archive(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        return ResponseEntity.ok(
                cardService.archiveCard(id, resolveUserId(userId))
        );
    }

    @PutMapping("/{id}/unarchive")
    public ResponseEntity<CardResponse> unarchive(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        return ResponseEntity.ok(
                cardService.unarchiveCard(id, resolveUserId(userId))
        );
    }

    @GetMapping("/board/{boardId}/archived")
    public ResponseEntity<List<CardResponse>> getArchivedByBoard(
            @PathVariable Long boardId
    ) {
        return ResponseEntity.ok(
                cardService.getArchivedCardsByBoard(boardId)
        );
    }

    @GetMapping("/list/{listId}/archived")
    public ResponseEntity<List<CardResponse>> getArchivedByList(
            @PathVariable Long listId
    ) {
        return ResponseEntity.ok(
                cardService.getArchivedCardsByList(listId)
        );
    }

    @PutMapping("/{id}/assignee")
    public ResponseEntity<CardResponse> setAssignment(
            @PathVariable Long id,
            @RequestBody AssignCardRequest request,
            @RequestHeader(value = "X-User-id", required = false) Long userId
    ) {
        return ResponseEntity.ok(
                cardService.setAssignee(id, request, resolveUserId(userId))
        );
    }

    @PutMapping("/{id}/priority")
    public ResponseEntity<CardResponse> setPriority(
            @PathVariable Long id,
            @Valid @RequestBody SetPriorityRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        return ResponseEntity.ok(
                cardService.setPriority(id, request, resolveUserId(userId))
        );
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<CardResponse> setStatus(
            @PathVariable Long id,
            @Valid @RequestBody SetStatusRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        return ResponseEntity.ok(
                cardService.setStatus(id, request, resolveUserId(userId))
        );
    }

    @GetMapping("/board/{boardId}/status/{status}")
    public ResponseEntity<List<CardResponse>> getByStatus(
            @PathVariable Long boardId,
            @PathVariable CardStatus status
    ) {
        return ResponseEntity.ok(
                cardService.getCardsByStatus(boardId, status)
        );
    }

    @GetMapping("/board/{boardId}/priority/{priority}")
    public ResponseEntity<List<CardResponse>> getByPriority(
            @PathVariable Long boardId,
            @PathVariable Priority priority
    ) {
        return ResponseEntity.ok(
                cardService.getCardsByPriority(boardId, priority)
        );
    }

    @GetMapping("/board/{boardId}/overdue")
    public ResponseEntity<List<CardResponse>> getOverdueByBoard(
            @PathVariable Long boardId
    ) {
        return ResponseEntity.ok(
                cardService.getOverdueCardsByBoard(boardId)
        );
    }

    @GetMapping("/overdue/all")
    public ResponseEntity<List<CardResponse>> getAllOverdue() {
        return ResponseEntity.ok(cardService.getAllOverdueCards());
    }

    @GetMapping("/board/{boardId}/search")
    public ResponseEntity<List<CardResponse>> search(
            @PathVariable Long boardId,
            @RequestParam String keyword
    ) {
        return ResponseEntity.ok(
                cardService.searchCards(boardId, keyword)
        );
    }

    @GetMapping("/search")
    public ResponseEntity<List<CardResponse>> searchGlobal(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long assigneeId
    ) {
        return ResponseEntity.ok(
                cardService.searchByTitleOrAssignee(keyword, assigneeId)
        );
    }

    @GetMapping("/{id}/activity")
    public ResponseEntity<List<CardActivityResponse>> getActivity(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(cardService.getCardActivity(id));
    }

    @PostMapping("/stats")
    public ResponseEntity<List<com.card.dto.BoardStatsResponse>> getBoardStats(
            @RequestBody List<Long> boardIds
    ) {
        return ResponseEntity.ok(cardService.getBoardStats(boardIds));
    }
}
