package com.flowboard.board.dto;


import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

import com.flowboard.board.entity.BoardMemberRole;
import com.flowboard.board.entity.BoardPriority;
import com.flowboard.board.entity.Visibility;

@Data
@Builder
public class BoardResponse {

    private Long id;
    private Long workspaceId;
    private String name;
    private String description;
    private String background;
    private Visibility visibility;
    private Long createdById;
    private boolean isClosed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime dueDate;
    private BoardPriority priority;
    private int memberCount;
    private List<MemberDTO> members;

    // Progress stats
    private long totalCards;
    private long doneCards;
    private int progressPercentage;

    private BoardAnalytics analytics;

    @Data
    @Builder
    public static class MemberDTO{
        private Long userId;
        private BoardMemberRole role;
        private LocalDateTime addedAt;
    }

    @Data
    @Builder
    public static class BoardAnalytics{
        private long totalMembers;
        private long observerCount;
        private long memberCount;
        private long adminCount;
    }
}
