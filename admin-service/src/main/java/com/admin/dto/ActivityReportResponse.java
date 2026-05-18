package com.admin.dto;

import java.util.List;

public class ActivityReportResponse {
    private String scope;
    private Long id;
    private long users;
    private long workspaces;
    private long boards;
    private long cards;
    private long overdueCards;
    private List<Object> auditLogs;

    public ActivityReportResponse() {
    }

    public ActivityReportResponse(String scope, Long id, long users, long workspaces, long boards,
                                  long cards, long overdueCards, List<Object> auditLogs) {
        this.scope = scope;
        this.id = id;
        this.users = users;
        this.workspaces = workspaces;
        this.boards = boards;
        this.cards = cards;
        this.overdueCards = overdueCards;
        this.auditLogs = auditLogs;
    }

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public long getUsers() { return users; }
    public void setUsers(long users) { this.users = users; }
    public long getWorkspaces() { return workspaces; }
    public void setWorkspaces(long workspaces) { this.workspaces = workspaces; }
    public long getBoards() { return boards; }
    public void setBoards(long boards) { this.boards = boards; }
    public long getCards() { return cards; }
    public void setCards(long cards) { this.cards = cards; }
    public long getOverdueCards() { return overdueCards; }
    public void setOverdueCards(long overdueCards) { this.overdueCards = overdueCards; }
    public List<Object> getAuditLogs() { return auditLogs; }
    public void setAuditLogs(List<Object> auditLogs) { this.auditLogs = auditLogs; }
}
