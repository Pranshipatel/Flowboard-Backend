package com.flowboard.workspace.dto;


import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

import com.flowboard.workspace.entity.MemberRole;
import com.flowboard.workspace.entity.Visibility;

@Data
@Builder
public class WorkspaceResponse {

    // Workspace details
    private Long id;
    private String name;
    private String description;
    private Long ownerId;
    private Visibility visibility;
    private String logoUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Members list
    private List<MemberDto> members;

    @Data
    @Builder
    public static class MemberDto {

        private Long userId;
        private MemberRole role;
        private LocalDateTime joinedAt;
    }
}