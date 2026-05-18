package com.flowboard.workspace.service;



import java.util.List;

import com.flowboard.workspace.dto.AddMemberRequest;
import com.flowboard.workspace.dto.CreateWorkspaceRequest;
import com.flowboard.workspace.dto.UpdateMemberRoleRequest;
import com.flowboard.workspace.dto.UpdateWorkspaceRequest;
import com.flowboard.workspace.dto.WorkspaceMemberResponse;
import com.flowboard.workspace.dto.WorkspaceResponse;

public interface WorkspaceService {

    // Create / Read / Update / Delete workspace
    WorkspaceResponse createWorkspace(CreateWorkspaceRequest request, Long ownerId);
    WorkspaceResponse getById(Long workspaceId, Long requesterId);
    List<WorkspaceResponse> getByOwner(Long ownerId);
    List<WorkspaceResponse> getByMember(Long userId);
    List<WorkspaceResponse> getPublicWorkspaces();
    List<WorkspaceResponse> getAllWorkspacesForAdmin();
    WorkspaceResponse updateWorkspace(Long workspaceId, UpdateWorkspaceRequest request, Long requesterId);
    void deleteWorkspace(Long workspaceId, Long requesterId);
    void deleteWorkspaceForAdmin(Long workspaceId);

    // Member management
    WorkspaceMemberResponse addMember(Long workspaceId, AddMemberRequest request, Long requesterId);
    void removeMember(Long workspaceId, Long userId, Long requesterId);
    void updateMemberRole(Long workspaceId, Long userId, UpdateMemberRoleRequest request, Long requesterId);
    List<WorkspaceMemberResponse> getMembers(Long workspaceId);
}
