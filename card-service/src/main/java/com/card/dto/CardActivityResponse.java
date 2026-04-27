package com.card.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CardActivityResponse {

    private Long id;

    private Long cardId;
    private Long actorId;

    private String actionType;
    private String description;

    private String oldValue;
    private String newValue;

    // Timestamp when the activity occurred
    private LocalDateTime createdAt;

}