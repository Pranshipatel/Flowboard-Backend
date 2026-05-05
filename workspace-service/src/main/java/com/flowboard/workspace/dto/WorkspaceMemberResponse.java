package com.flowboard.workspace.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WorkspaceMemberResponse {

    private Long id;
    private Long userId;
    private String role;
    private Long workspaceId;
}