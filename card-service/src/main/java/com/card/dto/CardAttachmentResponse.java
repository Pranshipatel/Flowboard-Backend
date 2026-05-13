package com.card.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CardAttachmentResponse {

    private Long id;
    private Long cardId;
    private Long uploadedById;
    private String fileName;
    private String contentType;
    private Long sizeBytes;
    private String url;
    private String resourceType;
    private LocalDateTime createdAt;
}
