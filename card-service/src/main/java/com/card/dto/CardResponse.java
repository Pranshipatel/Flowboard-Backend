package com.card.dto;


import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.card.entity.CardStatus;
import com.card.entity.Priority;

@Data
@Builder
public class CardResponse {

    private Long id;

    private Long listId;
    private Long boardId;

    private String title;
    private String description;

    private Integer position;

    private Priority priority;
    private CardStatus status;

    private LocalDate startDate;
    private LocalDate dueDate;

    private Long assigneeId;
    private Long createdById;

    private boolean isArchived;
    private boolean isOverdue;

    private String coverColor;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder.Default
    private List<CardAttachmentResponse> attachments = List.of();

}
