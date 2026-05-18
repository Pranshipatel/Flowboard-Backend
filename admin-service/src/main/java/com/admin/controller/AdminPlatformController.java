package com.admin.controller;

import com.admin.client.AuthAdminClient;
import com.admin.client.PlatformAdminClient;
import com.admin.dto.ActivityReportResponse;
import com.admin.dto.AdminUserResponse;
import com.admin.dto.BroadcastRequest;
import com.admin.security.AdminGuard;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminPlatformController {

    private final AuthAdminClient authAdminClient;
    private final PlatformAdminClient platformAdminClient;
    private final AdminGuard adminGuard;

    @GetMapping("/workspaces")
    public ResponseEntity<List<Object>> listWorkspaces(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        adminGuard.requirePlatformAdmin(role);
        return ResponseEntity.ok(platformAdminClient.listWorkspaces(authorization));
    }

    @DeleteMapping("/workspaces/{id}")
    public ResponseEntity<String> deleteWorkspace(
            @PathVariable Long id,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        adminGuard.requirePlatformAdmin(role);
        return ResponseEntity.ok(platformAdminClient.deleteWorkspace(id, authorization));
    }

    @GetMapping("/boards")
    public ResponseEntity<List<Object>> listBoards(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        adminGuard.requirePlatformAdmin(role);
        return ResponseEntity.ok(platformAdminClient.listBoards(authorization));
    }

    @PutMapping("/boards/{id}/close")
    public ResponseEntity<Object> closeBoard(
            @PathVariable Long id,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        adminGuard.requirePlatformAdmin(role);
        return ResponseEntity.ok(platformAdminClient.closeBoard(id, authorization));
    }

    @PutMapping("/boards/{id}/reopen")
    public ResponseEntity<Object> reopenBoard(
            @PathVariable Long id,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        adminGuard.requirePlatformAdmin(role);
        return ResponseEntity.ok(platformAdminClient.reopenBoard(id, authorization));
    }

    @DeleteMapping("/boards/{id}")
    public ResponseEntity<String> deleteBoard(
            @PathVariable Long id,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        adminGuard.requirePlatformAdmin(role);
        return ResponseEntity.ok(platformAdminClient.deleteBoard(id, authorization));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<List<Object>> auditLogs(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        adminGuard.requirePlatformAdmin(role);
        return ResponseEntity.ok(platformAdminClient.listAuditLogs(authorization));
    }

    @GetMapping("/overdue-cards")
    public ResponseEntity<List<Object>> overdueCards(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        adminGuard.requirePlatformAdmin(role);
        return ResponseEntity.ok(platformAdminClient.listOverdueCards(authorization));
    }

    @PostMapping("/broadcast")
    public ResponseEntity<List<Object>> broadcast(
            @RequestBody BroadcastRequest request,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestHeader(value = "X-User-Id", required = false) Long actorId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        adminGuard.requirePlatformAdmin(role);
        return ResponseEntity.ok(platformAdminClient.sendBroadcast(request, authorization, actorId));
    }

    @GetMapping("/activity-report")
    public ResponseEntity<ActivityReportResponse> report(
            @RequestParam(defaultValue = "platform") String scope,
            @RequestParam(required = false) Long id,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        adminGuard.requirePlatformAdmin(role);
        List<AdminUserResponse> users = authAdminClient.listUsers(authorization);
        List<Object> workspaces = platformAdminClient.listWorkspaces(authorization);
        List<Object> boards = platformAdminClient.listBoards(authorization);
        List<Object> overdue = platformAdminClient.listOverdueCards(authorization);
        List<Object> logs = platformAdminClient.listAuditLogs(authorization);
        long cardsCreated = logs.stream()
                .filter(log -> String.valueOf(((java.util.Map<?, ?>) log).get("actionType")).equalsIgnoreCase("CREATE"))
                .count();
        return ResponseEntity.ok(new ActivityReportResponse(
                scope,
                id,
                users.size(),
                workspaces.size(),
                boards.size(),
                cardsCreated,
                overdue.size(),
                logs
        ));
    }
}
