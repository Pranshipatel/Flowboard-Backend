package com.card.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "card_attachments",
        indexes = {
                @Index(name = "idx_card_attachment_card", columnList = "cardId"),
                @Index(name = "idx_card_attachment_public_id", columnList = "publicId")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long cardId;

    @Column(nullable = false)
    private Long uploadedById;

    @Column(nullable = false)
    private String fileName;

    private String contentType;

    private Long sizeBytes;

    @Column(nullable = false, length = 1000)
    private String url;

    @Column(nullable = false)
    private String publicId;

    @Column(nullable = false)
    private String resourceType;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
