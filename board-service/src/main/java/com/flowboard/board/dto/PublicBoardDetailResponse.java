package com.flowboard.board.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.flowboard.board.entity.BoardPriority;
import com.flowboard.board.entity.Visibility;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PublicBoardDetailResponse {
    private Long id;
    private Long workspaceId;
    private String name;
    private String description;
    private String background;
    private Visibility visibility;
    private boolean isClosed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime dueDate;
    private BoardPriority priority;
    private List<PublicListDto> lists;

    @Data
    @Builder
    public static class PublicListDto {
        private Long id;
        private Long boardId;
        private String name;
        private Integer position;
        private String color;
        private List<PublicCardDto> cards;
    }

    @Data
    @Builder
    public static class PublicCardDto {
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
}
