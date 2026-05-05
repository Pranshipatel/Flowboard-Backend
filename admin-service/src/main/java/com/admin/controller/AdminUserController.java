package com.admin.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.admin.client.AuthAdminClient;
import com.admin.dto.AdminUserResponse;
import com.admin.security.AdminGuard;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AuthAdminClient authAdminClient;
    private final AdminGuard adminGuard;

    @GetMapping
    public ResponseEntity<List<AdminUserResponse>> listUsers(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestHeader(value = "X-User-Role", required = false) String role
    ) {
        adminGuard.requirePlatformAdmin(role);
        return ResponseEntity.ok(authAdminClient.listUsers(authorization));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminUserResponse> getUser(
            @PathVariable Long id,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestHeader(value = "X-User-Role", required = false) String role
    ) {
        adminGuard.requirePlatformAdmin(role);
        return ResponseEntity.ok(authAdminClient.getUser(id, authorization));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(
            @PathVariable Long id,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestHeader(value = "X-User-Role", required = false) String role
    ) {
        adminGuard.requirePlatformAdmin(role);
        return ResponseEntity.ok(authAdminClient.deleteUser(id, authorization));
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<String> updateUserRole(
            @PathVariable Long id,
            @RequestParam String role,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestHeader(value = "X-User-Role", required = false) String adminRole
    ) {
        adminGuard.requirePlatformAdmin(adminRole);
        return ResponseEntity.ok(authAdminClient.updateUserRole(id, role, authorization));
    }

    @PutMapping("/{id}/suspend")
    public ResponseEntity<String> suspendUser(
            @PathVariable Long id,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestHeader(value = "X-User-Role", required = false) String adminRole
    ) {
        adminGuard.requirePlatformAdmin(adminRole);
        return ResponseEntity.ok(authAdminClient.suspendUser(id, authorization));
    }

    @PutMapping("/{id}/reactivate")
    public ResponseEntity<String> reactivateUser(
            @PathVariable Long id,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestHeader(value = "X-User-Role", required = false) String adminRole
    ) {
        adminGuard.requirePlatformAdmin(adminRole);
        return ResponseEntity.ok(authAdminClient.reactivateUser(id, authorization));
    }
}