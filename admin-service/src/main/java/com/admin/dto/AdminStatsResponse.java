package com.admin.dto;

public class AdminStatsResponse {
    private long totalUsers;
    private long totalWorkspaces;
    private long totalBoards;
    private long activeUsersToday;

    public AdminStatsResponse() {
    }

    public AdminStatsResponse(long totalUsers, long totalWorkspaces, long totalBoards, long activeUsersToday) {
        this.totalUsers = totalUsers;
        this.totalWorkspaces = totalWorkspaces;
        this.totalBoards = totalBoards;
        this.activeUsersToday = activeUsersToday;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getTotalWorkspaces() {
        return totalWorkspaces;
    }

    public void setTotalWorkspaces(long totalWorkspaces) {
        this.totalWorkspaces = totalWorkspaces;
    }

    public long getTotalBoards() {
        return totalBoards;
    }

    public void setTotalBoards(long totalBoards) {
        this.totalBoards = totalBoards;
    }

    public long getActiveUsersToday() {
        return activeUsersToday;
    }

    public void setActiveUsersToday(long activeUsersToday) {
        this.activeUsersToday = activeUsersToday;
    }
}
