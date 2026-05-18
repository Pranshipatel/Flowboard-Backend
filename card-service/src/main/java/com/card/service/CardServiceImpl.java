package com.card.service;



import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.card.dto.*;
import com.card.entity.*;

import com.card.exception.CustomException;
import com.card.repository.CardActivityRepository;
import com.card.repository.CardAttachmentRepository;
import com.card.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardServiceImpl implements CardService{

    private static final long FREE_CARD_LIMIT = 2;
    private static final long MAX_ATTACHMENT_BYTES = 20 * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain",
            "text/csv",
            "image/png",
            "image/jpeg",
            "image/webp"
    );

    private final CardRepository cardRepository;
    private final CardActivityRepository activityRepository;
    private final CardAttachmentRepository attachmentRepository;
    private final Cloudinary cloudinary;

    @Override
    @Transactional
    public CardResponse createCard(CreateCardRequest request, Long userId, boolean premium) {
        if (!premium && cardRepository.countByBoardIdAndIsArchivedFalse(request.getBoardId()) >= FREE_CARD_LIMIT) {
            throw new CustomException(
                    "Free users can create up to 2 cards per board. Upgrade to premium for unlimited cards.",
                    HttpStatus.FORBIDDEN
            );
        }

        int position;

        if(request.getPosition()!=null){
            cardRepository.shiftPositionsRight(
                    request.getListId(), request.getPosition());
            position = request.getPosition();
        }
        else{
            position= cardRepository.findMaxPositionByListId(request.getListId())
                    .map(max->max+1)
                    .orElse(0);
        }

        Card card = Card.builder()
                .listId(request.getListId())
                .boardId(request.getBoardId())
                .title(request.getTitle())
                .description(request.getDescription())
                .position(position)
                .priority(request.getPriority()!=null ?
                        request.getPriority() : Priority.MEDIUM)
                .status(CardStatus.TO_DO)
                .dueDate(request.getDueDate())
                .startDate(request.getStartDate())
                .assigneeId(request.getAssigneeId())
                .createdById(userId)
                .coverColor(request.getCoverColor())
                .isArchived(false)
                .createdAt(LocalDateTime.now())
                .build();

        cardRepository.save(card);

        logActivity(card.getId(), userId, "CREATE",
                "created card '"+card.getTitle()+"'",
                null, card.getTitle());

        if(card.getAssigneeId()!=null){
            logActivity(card.getId(), userId, "ASSIGNMENT",
                    "assigned card to userId="+card.getAssigneeId(),
                    null, String.valueOf(card.getAssigneeId()));
        }

        log.info("Card created: id={} title={} listId={} boardId={}",
                card.getId(), card.getTitle(), card.getListId(), card.getBoardId());
        return toResponse(card);
    }

    @Override
    public CardResponse getCardById(Long cardId) {
        return toResponse(findCard(cardId));
    }

    @Override
    public List<CardResponse> getCardByList(Long listId) {
        return cardRepository.findByListIdAndIsArchivedFalseOrderByPosition(listId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public List<CardResponse> getCardByBoard(Long boardId) {
        return cardRepository.findByBoardIdAndIsArchivedFalse(boardId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public List<CardResponse> getCardByAssignee(Long assigneeId) {
        return cardRepository.findByAssigneeIdAndIsArchivedFalse(assigneeId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public CardResponse updateCard(Long cardId, UpdateCardRequest request, Long userId) {
        Card card = findCard(cardId);

        if(card.isArchived()){
            throw new CustomException("Cannot updtade an archived card - unarchive it first",
                    HttpStatus.BAD_REQUEST);
        }

        if(request.getStatus()!=null &&
                request.getStatus()!=card.getStatus()){
            logActivity(cardId, userId, "STATUS_CHANGE", "changed status from "+card.getStatus()
            +" to "+request.getStatus(),
                    card.getStatus().name(), request.getStatus().name());
        }

        if(request.getPriority()!=null
            && request .getPriority()!=card.getPriority()){
            logActivity(cardId, userId, "PRIORITY_CHANGE",
                    "changed priority from "+ card.getPriority()
            + " to "+request.getPriority(),
                    card.getPriority().name(),
                    request.getPriority().name());
        }

        if(request.getDueDate()!=null
            && !request.getDueDate().equals(card.getDueDate())){
            logActivity(cardId, userId, "DUE_DATE_CHANGE",
                    "changed due date to "+request.getDueDate(),
                    card.getDueDate()!=null?card.getDueDate().toString(): null,
                    request.getDueDate().toString());
        }
        card.setTitle(request.getTitle());
        if (request.getDescription() != null)
            card.setDescription(request.getDescription());
        if (request.getPriority() != null)
            card.setPriority(request.getPriority());
        if (request.getStatus() != null)
            card.setStatus(request.getStatus());
        if (request.getDueDate() != null)
            card.setDueDate(request.getDueDate());
        if (request.getStartDate() != null)
            card.setStartDate(request.getStartDate());
        if (request.getCoverColor() != null)
            card.setCoverColor(request.getCoverColor());

        card.setUpdatedAt(LocalDateTime.now());
        cardRepository.save(card);

        log.info("Card updated: id={}", cardId);
        return toResponse(card);
    }

    @Override
    @Transactional
    public void deleteCard(Long cardId, Long userId) {
            Card card = findCard(cardId);

            if(!card.isArchived()){
                cardRepository.shiftPositionsLeft(card.getListId(), card.getPosition());
            }

            attachmentRepository.findByCardIdOrderByCreatedAtDesc(cardId)
                    .forEach(this::deleteFromCloudinaryQuietly);
            attachmentRepository.deleteByCardId(cardId);
            cardRepository.delete(card);
            log.info("Card deleted: id={} by userId={}", cardId, userId);
    }

    @Override
    @Transactional
    public CardResponse moveCard(Long cardId,
                                 MoveCardRequest request,
                                 Long userId) {
        Card card = findCard(cardId);

        Long sourceListId = card.getListId();
        Long targetListId = request.getTargetListId();

        String oldLocation = "listId=" + sourceListId;
        String newLocation = "listId=" + targetListId;

        // Step 1: close gap in source list
        cardRepository.shiftPositionsLeft(sourceListId, card.getPosition());

        // Step 2: make room in target list
        int targetPosition;
        if (request.getTargetPosition() != null) {
            cardRepository.shiftPositionsRight(
                    targetListId, request.getTargetPosition());
            targetPosition = request.getTargetPosition();
        } else {
            targetPosition = cardRepository
                    .findMaxPositionByListId(targetListId)
                    .map(max -> max + 1)
                    .orElse(0);
        }

        // Step 3: update card
        card.setListId(targetListId);
        card.setBoardId(request.getTargetBoardId());
        card.setPosition(targetPosition);
        card.setUpdatedAt(LocalDateTime.now());
        cardRepository.save(card);

        logActivity(cardId, userId, "MOVE",
                "moved card from " + oldLocation + " to " + newLocation,
                oldLocation, newLocation);

        log.info("Card moved: id={} from listId={} to listId={}",
                cardId, sourceListId, targetListId);

        return toResponse(card);
    }

    @Override
    @Transactional
    public List<CardResponse> reorderCards(ReorderCardRequest request,
                                           Long userId) {
        List<Long> orderedIds = request.getOrderedCardIds();

        List<Card> listCards = cardRepository
                .findByListIdAndIsArchivedFalseOrderByPosition(request.getListId());

        List<Long> existingIds = listCards.stream()
                .map(Card::getId).toList();

        for (Long id : orderedIds) {
            if (!existingIds.contains(id)) {
                throw new CustomException(
                        "Card id=" + id + " does not belong to list id="
                                + request.getListId(),
                        HttpStatus.BAD_REQUEST);
            }
        }

        AtomicInteger pos = new AtomicInteger(0);
        orderedIds.forEach(id -> {
            Card card = listCards.stream()
                    .filter(c -> c.getId().equals(id))
                    .findFirst().orElseThrow();
            card.setPosition(pos.getAndIncrement());
            card.setUpdatedAt(LocalDateTime.now());
            cardRepository.save(card);
        });

        log.info("Cards reordered in listId={}", request.getListId());

        return cardRepository
                .findByListIdAndIsArchivedFalseOrderByPosition(request.getListId())
                .stream().map(this::toResponse).toList();
    }

    // ── Archive ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public CardResponse archiveCard(Long cardId, Long userId) {
        Card card = findCard(cardId);

        if (card.isArchived()) {
            throw new CustomException(
                    "Card is already archived", HttpStatus.BAD_REQUEST);
        }

        cardRepository.shiftPositionsLeft(card.getListId(), card.getPosition());

        card.setArchived(true);
        card.setUpdatedAt(LocalDateTime.now());
        cardRepository.save(card);

        logActivity(cardId, userId, "ARCHIVE",
                "archived card", null, "archived");

        log.info("Card archived: id={}", cardId);
        return toResponse(card);
    }

    @Override
    @Transactional
    public CardResponse unarchiveCard(Long cardId, Long userId) {
        Card card = findCard(cardId);

        if (!card.isArchived()) {
            throw new CustomException(
                    "Card is not archived", HttpStatus.BAD_REQUEST);
        }

        int newPosition = cardRepository
                .findMaxPositionByListId(card.getListId())
                .map(max -> max + 1)
                .orElse(0);

        card.setArchived(false);
        card.setPosition(newPosition);
        card.setUpdatedAt(LocalDateTime.now());
        cardRepository.save(card);

        logActivity(cardId, userId, "UNARCHIVE",
                "unarchived card", "archived", null);

        log.info("Card unarchived: id={} newPosition={}", cardId, newPosition);
        return toResponse(card);
    }

    @Override
    public List<CardResponse> getArchivedCardsByBoard(Long boardId) {
        return cardRepository.findByBoardIdAndIsArchivedTrue(boardId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public List<CardResponse> getArchivedCardsByList(Long listId) {
        return cardRepository.findByListIdAndIsArchivedTrue(listId)
                .stream().map(this::toResponse).toList();
    }

    // ── Assignment ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public CardResponse setAssignee(Long cardId,
                                    AssignCardRequest request,
                                    Long userId) {
        Card card = findCard(cardId);

        String oldAssignee = card.getAssigneeId() != null
                ? String.valueOf(card.getAssigneeId()) : "none";
        String newAssignee = request.getAssigneeId() != null
                ? String.valueOf(request.getAssigneeId()) : "none";

        card.setAssigneeId(request.getAssigneeId());
        card.setUpdatedAt(LocalDateTime.now());
        cardRepository.save(card);

        logActivity(cardId, userId, "ASSIGNMENT",
                request.getAssigneeId() != null
                        ? "assigned card to userId=" + request.getAssigneeId()
                        : "unassigned card",
                oldAssignee, newAssignee);

        log.info("Card assignee updated: id={} assigneeId={}",
                cardId, request.getAssigneeId());
        return toResponse(card);
    }

    // ── Priority and Status ───────────────────────────────────────────────────

    @Override
    @Transactional
    public CardResponse setPriority(Long cardId,
                                    SetPriorityRequest request,
                                    Long userId) {
        Card card = findCard(cardId);
        String old = card.getPriority().name();

        card.setPriority(request.getPriority());
        card.setUpdatedAt(LocalDateTime.now());
        cardRepository.save(card);

        logActivity(cardId, userId, "PRIORITY_CHANGE",
                "changed priority from " + old
                        + " to " + request.getPriority(),
                old, request.getPriority().name());

        return toResponse(card);
    }

    @Override
    @Transactional
    public CardResponse setStatus(Long cardId,
                                  SetStatusRequest request,
                                  Long userId) {
        Card card = findCard(cardId);
        String old = card.getStatus().name();

        card.setStatus(request.getStatus());
        card.setUpdatedAt(LocalDateTime.now());
        cardRepository.save(card);

        logActivity(cardId, userId, "STATUS_CHANGE",
                "changed status from " + old
                        + " to " + request.getStatus(),
                old, request.getStatus().name());

        return toResponse(card);
    }

    // ── Filtering ─────────────────────────────────────────────────────────────

    @Override
    public List<CardResponse> getCardsByStatus(Long boardId, CardStatus status) {
        return cardRepository
                .findByBoardIdAndStatusAndIsArchivedFalse(boardId, status)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public List<CardResponse> getCardsByPriority(Long boardId, Priority priority) {
        return cardRepository
                .findByBoardIdAndPriorityAndIsArchivedFalse(boardId, priority)
                .stream().map(this::toResponse).toList();
    }

    // ── Overdue ───────────────────────────────────────────────────────────────

    @Override
    public List<CardResponse> getOverdueCardsByBoard(Long boardId) {
        return cardRepository
                .findOverdueByBoardId(boardId, LocalDate.now())
                .stream().map(this::toResponse).toList();
    }

    @Override
    public List<CardResponse> getAllOverdueCards() {
        return cardRepository
                .findAllOverdue(LocalDate.now())
                .stream().map(this::toResponse).toList();
    }

    // ── Search ────────────────────────────────────────────────────────────────

    @Override
    public List<CardResponse> searchCards(Long boardId, String keyword) {
        return cardRepository
                .searchByTitle(boardId, keyword)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public List<CardResponse> searchByTitleOrAssignee(String keyword,
                                                      Long assigneeId) {
        return cardRepository
                .searchByTitleOrAssignee(keyword, assigneeId)
                .stream().map(this::toResponse).toList();
    }

    // ── Activity log ──────────────────────────────────────────────────────────

    @Override
    public List<CardActivityResponse> getCardActivity(Long cardId) {
        findCard(cardId); // validate card exists
        return activityRepository
                .findByCardIdOrderByCreatedAtDesc(cardId)
                .stream()
                .map(this::toActivityResponse)
                .toList();
    }

    @Override
    public List<CardActivityResponse> getAllActivityForAdmin() {
        return activityRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toActivityResponse)
                .toList();
    }

    @Override
    @Transactional
    public CardAttachmentResponse uploadAttachment(Long cardId, MultipartFile file, Long userId) {
        Card card = findCard(cardId);
        if (card.isArchived()) {
            throw new CustomException("Cannot attach files to an archived card", HttpStatus.BAD_REQUEST);
        }
        validateAttachment(file);

        try {
            Map<?, ?> upload = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "resource_type", "auto",
                    "folder", "flowboard/cards/" + cardId,
                    "use_filename", true,
                    "unique_filename", true
            ));

            Object resourceType = upload.get("resource_type");
            CardAttachment attachment = CardAttachment.builder()
                    .cardId(cardId)
                    .uploadedById(userId)
                    .fileName(safeOriginalFileName(file))
                    .contentType(file.getContentType())
                    .sizeBytes(file.getSize())
                    .url(String.valueOf(upload.get("secure_url")))
                    .publicId(String.valueOf(upload.get("public_id")))
                    .resourceType(resourceType != null ? String.valueOf(resourceType) : "raw")
                    .createdAt(LocalDateTime.now())
                    .build();

            attachmentRepository.save(attachment);
            logActivity(cardId, userId, "ATTACHMENT_ADD",
                    "attached file '" + attachment.getFileName() + "'",
                    null, attachment.getFileName());
            return toAttachmentResponse(attachment);
        } catch (IOException | RuntimeException e) {
            throw new CustomException("Failed to upload attachment", HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    public List<CardAttachmentResponse> getAttachments(Long cardId) {
        findCard(cardId);
        return attachmentRepository.findByCardIdOrderByCreatedAtDesc(cardId)
                .stream()
                .map(this::toAttachmentResponse)
                .toList();
    }

    @Override
    @Transactional
    public void deleteAttachment(Long cardId, Long attachmentId, Long userId) {
        findCard(cardId);
        CardAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new CustomException("Attachment not found", HttpStatus.NOT_FOUND));
        if (!attachment.getCardId().equals(cardId)) {
            throw new CustomException("Attachment does not belong to this card", HttpStatus.BAD_REQUEST);
        }

        deleteFromCloudinaryQuietly(attachment);
        attachmentRepository.delete(attachment);
        logActivity(cardId, userId, "ATTACHMENT_DELETE",
                "removed file '" + attachment.getFileName() + "'",
                attachment.getFileName(), null);
    }

    private Card findCard(Long cardId){
        return cardRepository.findById(cardId)
                .orElseThrow(()-> new CustomException("Card not found", HttpStatus.NOT_FOUND));
    }
    private void logActivity(Long cardId, Long actorId,
                             String actionType, String description,
                             String oldValue, String newValue){
        CardActivity activity = CardActivity.builder()
                .cardId(cardId)
                .actorId(actorId)
                .actionType(actionType)
                .description(description)
                .oldValue(oldValue)
                .newValue(newValue)
                .createdAt(LocalDateTime.now())
                .build();

        activityRepository.save(activity);
    }

    private CardResponse toResponse(Card card){
        card.computeOverdue();
        return CardResponse.builder()
                .id(card.getId())
                .listId(card.getListId())
                .boardId(card.getBoardId())
                .title(card.getTitle())
                .description(card.getDescription())
                .position(card.getPosition())
                .priority(card.getPriority())
                .status(card.getStatus())
                .dueDate(card.getDueDate())
                .startDate(card.getStartDate())
                .assigneeId(card.getAssigneeId())
                .createdById(card.getCreatedById())
                .isArchived(card.isArchived())
                .isOverdue(card.isOverdue())
                .coverColor(card.getCoverColor())
                .createdAt(card.getCreatedAt())
                .updatedAt(card.getUpdatedAt())
                .attachments(attachmentRepository.findByCardIdOrderByCreatedAtDesc(card.getId())
                        .stream()
                        .map(this::toAttachmentResponse)
                        .toList())
                .build();
    }

    private CardAttachmentResponse toAttachmentResponse(CardAttachment a) {
        return CardAttachmentResponse.builder()
                .id(a.getId())
                .cardId(a.getCardId())
                .uploadedById(a.getUploadedById())
                .fileName(a.getFileName())
                .contentType(a.getContentType())
                .sizeBytes(a.getSizeBytes())
                .url(a.getUrl())
                .resourceType(a.getResourceType())
                .createdAt(a.getCreatedAt())
                .build();
    }

    private CardActivityResponse toActivityResponse(CardActivity a){
        return CardActivityResponse.builder()
                .id(a.getId())
                .cardId(a.getCardId())
                .actorId(a.getActorId())
                .actionType(a.getActionType())
                .description(a.getDescription())
                .oldValue(a.getOldValue())
                .newValue(a.getNewValue())
                .createdAt(a.getCreatedAt())
                .build();
    }

    private void validateAttachment(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException("Please choose a file to upload", HttpStatus.BAD_REQUEST);
        }
        if (file.getSize() > MAX_ATTACHMENT_BYTES) {
            throw new CustomException("File size must be 20MB or less", HttpStatus.BAD_REQUEST);
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new CustomException("Unsupported file type", HttpStatus.BAD_REQUEST);
        }
    }

    private String safeOriginalFileName(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isBlank()) {
            return "attachment";
        }
        return fileName.replace("\\", "_").replace("/", "_");
    }

    private void deleteFromCloudinaryQuietly(CardAttachment attachment) {
        try {
            cloudinary.uploader().destroy(attachment.getPublicId(), ObjectUtils.asMap(
                    "resource_type", attachment.getResourceType()
            ));
        } catch (IOException | RuntimeException e) {
            log.warn("Could not delete attachment from Cloudinary: id={} publicId={}",
                    attachment.getId(), attachment.getPublicId());
        }
    }

    @Override
    public List<BoardStatsResponse> getBoardStats(List<Long> boardIds) {
        if (boardIds == null || boardIds.isEmpty()) return List.of();
        List<Object[]> results = cardRepository.getStatsByBoardIds(boardIds);
        return results.stream().map(row -> BoardStatsResponse.builder()
                .boardId(((Number) row[0]).longValue())
                .totalCards(((Number) row[1]).longValue())
                .doneCards(((Number) row[2]).longValue())
                .build()).toList();
    }
}
