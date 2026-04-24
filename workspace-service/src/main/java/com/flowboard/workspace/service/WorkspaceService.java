package com.flowboard.workspace.service;



import java.util.List;

import com.flowboard.workspace.dto.AddMemberRequest;
import com.flowboard.workspace.dto.CreateWorkspaceRequest;
import com.flowboard.workspace.dto.UpdateMemberRoleRequest;
import com.flowboard.workspace.dto.UpdateWorkspaceRequest;
import com.flowboard.workspace.dto.WorkspaceResponse;
import com.flowboard.workspace.entity.WorkspaceMember;

public interface WorkspaceService {

    // Create / Read / Update / Delete workspace
    WorkspaceResponse createWorkspace(CreateWorkspaceRequest request, Long ownerId);
    WorkspaceResponse getById(Long workspaceId, Long requesterId);
    List<WorkspaceResponse> getByOwner(Long ownerId);
    List<WorkspaceResponse> getByMember(Long userId);
    List<WorkspaceResponse> getPublicWorkspaces();
    WorkspaceResponse updateWorkspace(Long workspaceId, UpdateWorkspaceRequest request, Long requesterId);
    void deleteWorkspace(Long workspaceId, Long requesterId);

    // Member management
    WorkspaceMember addMember(Long workspaceId, AddMemberRequest request, Long requesterId);
    void removeMember(Long workspaceId, Long userId, Long requesterId);
    void updateMemberRole(Long workspaceId, Long userId, UpdateMemberRoleRequest requesr, Long requesterId);
    List<WorkspaceMember> getMembers(Long workspaceId);
}