package com.admin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.admin.client.AuthAdminClient;
import com.admin.dto.AdminStatsResponse;
import com.admin.dto.AdminUserResponse;
import com.admin.security.AdminGuard;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {

    private final AuthAdminClient authAdminClient;
    private final AdminGuard adminGuard;

    @GetMapping
    public ResponseEntity<AdminStatsResponse> getStats(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestHeader(value = "X-User-Role", required = false) String role
    ) {
        adminGuard.requirePlatformAdmin(role);
        
        List<AdminUserResponse> users = authAdminClient.listUsers(authorization);
        
        AdminStatsResponse stats = AdminStatsResponse.builder()
                .totalUsers(users.size())
                .totalWorkspaces(12) // Mocked for now
                .totalBoards(45)     // Mocked for now
                .activeUsersToday(users.stream().filter(AdminUserResponse::isActive).count())
                .build();
                
        return ResponseEntity.ok(stats);
    }
}
