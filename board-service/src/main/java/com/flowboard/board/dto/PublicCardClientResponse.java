package com.flowboard.board.dto;

import java.time.LocalDate;

import lombok.Data;

@Data
public class PublicCardClientResponse {
    private Long id;
    private Long listId;
    private Long boardId;
    private String title;
    private String description;
    private Integer position;
    private String priority;
    private String status;
    private LocalDate startDate;
    private LocalDate dueDate;
    private boolean isOverdue;
    private String coverColor;
}
