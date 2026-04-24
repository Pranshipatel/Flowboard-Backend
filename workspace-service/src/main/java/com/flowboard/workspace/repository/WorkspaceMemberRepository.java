package com.flowboard.workspace.repository;


import org.springframework.data.jpa.repository.JpaRepository;

import com.flowboard.workspace.entity.MemberRole;
import com.flowboard.workspace.entity.WorkspaceMember;

import java.util.List;
import java.util.Optional;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, Long> {

    // Get all members of a workspace
    List<WorkspaceMember> findByWorkspaceId(Long workspaceId);

    // Get specific member by workspace + user
    Optional<WorkspaceMember> findByWorkspaceIdAndUserId(Long workspaceId, Long userId);

    // Check if user is part of workspace
    boolean existsByWorkspaceIdAndUserId(Long workspaceId, Long userId);

    // Remove user from workspace
    void deleteByWorkspaceIdAndUserId(Long workspaceId, Long userId);

    // Get members by role
    List<WorkspaceMember> findByWorkspaceIdAndRole(Long workspaceId, MemberRole role);
}