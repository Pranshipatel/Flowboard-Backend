package com.card.service;


import com.card.dto.*;
import com.card.entity.CardStatus;
import com.card.entity.Priority;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CardService {

    // Create & Fetch
    CardResponse createCard(CreateCardRequest request, Long userId, boolean premium);
    CardResponse getCardById(Long cardId);

    List<CardResponse> getCardByList(Long listId);
    List<CardResponse> getCardByBoard(Long boardId);
    List<CardResponse> getCardByAssignee(Long assigneeId);

    // Update & Delete
    CardResponse updateCard(Long cardId, UpdateCardRequest request, Long userId);
    void deleteCard(Long cardId, Long userId);

    // Movement
    CardResponse moveCard(Long cardId, MoveCardRequest request, Long userId);
    List<CardResponse> reorderCards(ReorderCardRequest request, Long userId);

    // Archive
    CardResponse archiveCard(Long cardId, Long userId);
    CardResponse unarchiveCard(Long cardId, Long userId);

    List<CardResponse> getArchivedCardsByBoard(Long boardId);
    List<CardResponse> getArchivedCardsByList(Long listId);

    // Assignment & State
    CardResponse setAssignee(Long cardId, AssignCardRequest request, Long userId);
    CardResponse setPriority(Long cardId, SetPriorityRequest request, Long userId);
    CardResponse setStatus(Long cardId, SetStatusRequest request, Long userId);

    List<CardResponse> getCardsByStatus(Long boardId, CardStatus status);
    List<CardResponse> getCardsByPriority(Long boardId, Priority priority);

    // Deadlines
    List<CardResponse> getOverdueCardsByBoard(Long boardId);
    List<CardResponse> getAllOverdueCards();

    // Search
    List<CardResponse> searchCards(Long boardId, String keyword);
    List<CardResponse> searchByTitleOrAssignee(String keyword, Long assigneeId);

    // Activity
    List<CardActivityResponse> getCardActivity(Long cardId);

    // Attachments
    CardAttachmentResponse uploadAttachment(Long cardId, MultipartFile file, Long userId);
    List<CardAttachmentResponse> getAttachments(Long cardId);
    void deleteAttachment(Long cardId, Long attachmentId, Long userId);

    // Board Stats
    List<BoardStatsResponse> getBoardStats(List<Long> boardIds);
}
