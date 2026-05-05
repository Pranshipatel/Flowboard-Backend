package com.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdminStatsResponse {
    private long totalUsers;
    private long totalWorkspaces;
    private long totalBoards;
    private long activeUsersToday;
}
