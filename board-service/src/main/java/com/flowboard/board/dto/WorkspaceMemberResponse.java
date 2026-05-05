package com.flowboard.board.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceMemberResponse {
    private Long id;
    private Long workspaceId;
    private Long userId;
    private String role;
}
