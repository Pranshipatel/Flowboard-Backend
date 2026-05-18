package com.flowboard.workspace.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.flowboard.workspace.dto.AddMemberRequest;
import com.flowboard.workspace.dto.CreateWorkspaceRequest;
import com.flowboard.workspace.dto.UpdateMemberRoleRequest;
import com.flowboard.workspace.dto.UpdateWorkspaceRequest;
import com.flowboard.workspace.dto.WorkspaceMemberResponse;
import com.flowboard.workspace.dto.WorkspaceResponse;
import com.flowboard.workspace.service.WorkspaceService;

import java.util.List;

/**
 * Workspace Controller
 */
@RestController
@RequestMapping("/api/v1/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    private void requirePlatformAdmin(String role) {
        if (!"PLATFORM_ADMIN".equalsIgnoreCase(role)) {
            throw new com.flowboard.workspace.exception.CustomException(
                    "Platform admin access required",
                    org.springframework.http.HttpStatus.FORBIDDEN);
        }
    }

    @GetMapping("/admin")
    public ResponseEntity<List<WorkspaceResponse>> getAllForAdmin(
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        requirePlatformAdmin(role);
        return ResponseEntity.ok(workspaceService.getAllWorkspacesForAdmin());
    }

    @DeleteMapping("/admin/{id}")
    public ResponseEntity<String> deleteForAdmin(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        requirePlatformAdmin(role);
        workspaceService.deleteWorkspaceForAdmin(id);
        return ResponseEntity.ok("Workspace deleted successfully");
    }

    // Create workspace
    @PostMapping
    public ResponseEntity<WorkspaceResponse> create(
            @Valid @RequestBody CreateWorkspaceRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader
    ){
        Long userId = resolveUserId(userIdHeader);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workspaceService.createWorkspace(request, userId));
    }

    // Get workspace by ID
    @GetMapping("/{id}")
    public ResponseEntity<WorkspaceResponse> getById(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader) {

        return ResponseEntity.ok(workspaceService.getById(id, userIdHeader));
    }

    // Get by owner
    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<List<WorkspaceResponse>> getByOwner(@PathVariable Long ownerId){
        return ResponseEntity.ok(workspaceService.getByOwner(ownerId));
    }

    // Get by member
    @GetMapping("/member/{userId}")
    public ResponseEntity<List<WorkspaceResponse>> getByMember(@PathVariable Long userId){
        return ResponseEntity.ok(workspaceService.getByMember(userId));
    }

    // Get public workspaces
    @GetMapping("/public")
    public ResponseEntity<List<WorkspaceResponse>> getPublic(){
        return ResponseEntity.ok(workspaceService.getPublicWorkspaces());
    }

    // Update workspace
    @PutMapping("/{id}")
    public ResponseEntity<WorkspaceResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateWorkspaceRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader) {

        Long userId = resolveUserId(userIdHeader);
        return ResponseEntity.ok(workspaceService.updateWorkspace(id, request, userId));
    }

    // Delete workspace
    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader) {

        Long userId = resolveUserId(userIdHeader);
        workspaceService.deleteWorkspace(id, userId);

        return ResponseEntity.ok("Workspace deleted successfully");
    }

    // Add member
    @PostMapping("/{id}/members")
    public ResponseEntity<WorkspaceMemberResponse> addMember(
            @PathVariable Long id,
            @Valid @RequestBody AddMemberRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader) {

        Long userId = resolveUserId(userIdHeader);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workspaceService.addMember(id, request, userId));
    }

    // Remove member
    @DeleteMapping("/{id}/members/{memberId}")
    public ResponseEntity<String> removeMember(
            @PathVariable Long id,
            @PathVariable Long memberId,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader) {

        Long userId = resolveUserId(userIdHeader);
        workspaceService.removeMember(id, memberId, userId);

        return ResponseEntity.ok("Member removed successfully");
    }

    // Update member role
    @PutMapping("/{id}/members/{memberId}/role")
    public ResponseEntity<String> updateMemberRole(
            @PathVariable Long id,
            @PathVariable Long memberId,
            @Valid @RequestBody UpdateMemberRoleRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader) {

        Long userId = resolveUserId(userIdHeader);
        workspaceService.updateMemberRole(id, memberId, request, userId);

        return ResponseEntity.ok("Member role updated successfully");
    }

    // Get members
    @GetMapping("/{id}/members")
    public ResponseEntity<List<WorkspaceMemberResponse>> getMembers(@PathVariable Long id) {
        return ResponseEntity.ok(workspaceService.getMembers(id));
    }

    // Resolve user ID from headers
    private Long resolveUserId(Long userIdHeader){

        if(userIdHeader != null){
            return userIdHeader;
        }

        throw new com.flowboard.workspace.exception.CustomException(
                "X-User-Id header is required",
                org.springframework.http.HttpStatus.BAD_REQUEST
        );
    }
}
