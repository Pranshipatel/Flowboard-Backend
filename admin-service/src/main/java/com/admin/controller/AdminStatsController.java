package com.admin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.admin.client.AuthAdminClient;
import com.admin.client.PlatformAdminClient;
import com.admin.dto.AdminStatsResponse;
import com.admin.dto.AdminUserResponse;
import com.admin.security.AdminGuard;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {

    private final AuthAdminClient authAdminClient;
    private final PlatformAdminClient platformAdminClient;
    private final AdminGuard adminGuard;

    @GetMapping
    public ResponseEntity<AdminStatsResponse> getStats(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestHeader(value = "X-User-Role", required = false) String role
    ) {
        adminGuard.requirePlatformAdmin(role);
        
        List<AdminUserResponse> users = authAdminClient.listUsers(authorization);
        List<Object> workspaces = platformAdminClient.listWorkspaces(authorization);
        List<Object> boards = platformAdminClient.listBoards(authorization);
        List<Object> auditLogs = platformAdminClient.listAuditLogs(authorization);
        long cardsCreated = auditLogs.stream()
                .filter(log -> String.valueOf(((java.util.Map<?, ?>) log).get("actionType")).equalsIgnoreCase("CREATE"))
                .count();
        
        AdminStatsResponse stats = new AdminStatsResponse(
                users.size(),
                workspaces.size(),
                boards.size(),
                cardsCreated,
                workspaces.size()
        );
                
        return ResponseEntity.ok(stats);
    }
}
