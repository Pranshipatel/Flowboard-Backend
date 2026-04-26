package com.flowboard.list.dto;


import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ListResponse {

    // Basic Info
    private Long id;
    private Long boardId;
    private String name;

    // Position & Appearance
    private Integer position;
    private String color;

    // Status
    private boolean isArchived;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Additional Info
    private int cardCount;
}