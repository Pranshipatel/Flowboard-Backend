package com.flowboard.workspace.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.jdbc.Work;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.flowboard.workspace.dto.AddMemberRequest;
import com.flowboard.workspace.dto.CreateWorkspaceRequest;
import com.flowboard.workspace.dto.UpdateMemberRoleRequest;
import com.flowboard.workspace.dto.UpdateWorkspaceRequest;
import com.flowboard.workspace.dto.WorkspaceResponse;
import com.flowboard.workspace.entity.MemberRole;
import com.flowboard.workspace.entity.Visibility;
import com.flowboard.workspace.entity.Workspace;
import com.flowboard.workspace.entity.WorkspaceMember;
import com.flowboard.workspace.exception.CustomException;
import com.flowboard.workspace.repository.WorkspaceMemberRepository;
import com.flowboard.workspace.repository.WorkspaceRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Workspace Service Implementation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkspaceServiceImpl implements WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;

    // Create workspace
    @Override
    @Transactional
    public WorkspaceResponse createWorkspace(CreateWorkspaceRequest request, Long ownerId){

        if(workspaceRepository.existsByNameAndOwnerId(request.getName(), ownerId)){
            throw new CustomException("You already have a workspace named '"+request.getName()+"'",
                    HttpStatus.BAD_REQUEST);
        }

        Workspace workspace = Workspace.builder()
                .name(request.getName())
                .description(request.getDescription())
                .ownerId(ownerId)
                .visibility(request.getVisibility()!=null ?request.getVisibility(): Visibility.PRIVATE)
                .logoUrl(request.getLogoUrl())
                .createdAt(LocalDateTime.now())
                .build();

        workspaceRepository.save(workspace);

        // Add owner as ADMIN member
        WorkspaceMember ownerMember = WorkspaceMember.builder()
                .workspace(workspace)
                .userId(ownerId)
                .role(MemberRole.ADMIN)
                .joinedAt(LocalDateTime.now())
                .build();

        memberRepository.save(ownerMember);

        log.info("Workspace created: id={} name={} owner={}", workspace.getId(), workspace.getName(), ownerId);
        return toResponse(workspace);
    }

    // Get workspace by ID
    @Override
    public WorkspaceResponse getById(Long workspaceId, Long requesterId){
        Workspace workspace = findWorkspace(workspaceId);

        if(workspace.getVisibility()==Visibility.PRIVATE){
            requireMember(workspaceId, requesterId);
        }

        return toResponse(workspace);
    }

    @Override
    public List<WorkspaceResponse> getByOwner(Long ownerId){
        return workspaceRepository.findByOwnerId(ownerId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public List<WorkspaceResponse> getByMember(Long userId){
        return workspaceRepository.findByMemberUserId(userId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public List<WorkspaceResponse> getPublicWorkspaces() {
        return workspaceRepository.findByVisibility(Visibility.PUBLIC)
                .stream().map(this::toResponse).toList();
    }

    // Update workspace
    @Override
    @Transactional
    public WorkspaceResponse updateWorkspace(Long workspaceId,
                                             UpdateWorkspaceRequest request,
                                             Long requesterId){

        Workspace workspace = findWorkspace(workspaceId);
        requireAdmin(workspaceId, requesterId);

        workspace.setName(request.getName());
        workspace.setDescription(request.getDescription());

        if(request.getVisibility()!=null){
            workspace.setLogoUrl(request.getLogoUrl());
        }

        workspace.setUpdatedAt(LocalDateTime.now());
        workspaceRepository.save(workspace);

        log.info("Workspace updated: id={}", workspaceId);
        return toResponse(workspace);
    }

    // Delete workspace (owner only)
    @Override
    @Transactional
    public void deleteWorkspace(Long workspaceId, Long requesterId){

        Workspace workspace = findWorkspace(workspaceId);

        if(!workspace.getOwnerId().equals(requesterId)){
            throw new CustomException("Only the workspace owner can delete it", HttpStatus.FORBIDDEN);
        }

        workspaceRepository.delete(workspace);
        log.info("Workspace deleted: id={}", workspaceId);
    }

    // Add member
    @Override
    @Transactional
    public WorkspaceMember addMember(Long workspaceId,
                                     AddMemberRequest request,
                                     Long requesterId){

        findWorkspace(workspaceId);
        requireAdmin(workspaceId, requesterId);

        if(memberRepository.existsByWorkspaceIdAndUserId(workspaceId, request.getUserId())){
            throw new CustomException("User is already a member of this workspace", HttpStatus.BAD_REQUEST);
        }

        Workspace workspace = findWorkspace(workspaceId);

        WorkspaceMember member = WorkspaceMember.builder()
                .workspace(workspace)
                .userId(request.getUserId())
                .role(request.getRole()!=null ? request.getRole() : MemberRole.MEMBER)
                .joinedAt(LocalDateTime.now())
                .build();

        memberRepository.save(member);

        log.info("Member added: workspaceId={} userId={} role={}",
                workspaceId, request.getUserId(), member.getRole());

        return member;
    }

    // Remove member
    @Override
    @Transactional
    public void removeMember(Long workspaceId, Long userId, Long requesterid){

        findWorkspace(workspaceId);
        requireAdmin(workspaceId, requesterid);

        if(!memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)){
            throw new CustomException("User is not a member of this workspace", HttpStatus.NOT_FOUND);
        }

        Workspace workspace = findWorkspace(workspaceId);

        if(workspace.getOwnerId().equals(userId)){
            throw new CustomException("Cannot remove the workspace owner", HttpStatus.BAD_REQUEST);
        }

        memberRepository.deleteByWorkspaceIdAndUserId(workspaceId, userId);

        log.info("Member removed: workspaceId={} userId={}", workspaceId, userId);
    }

    // Update member role
    @Override
    @Transactional
    public void updateMemberRole(Long workspaceId, Long userId,
                                 UpdateMemberRoleRequest request, Long requesterId){

        findWorkspace(workspaceId);
        requireAdmin(workspaceId, requesterId);

        WorkspaceMember member = memberRepository
                .findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new CustomException(
                        "User is not the member of this workspace", HttpStatus.NOT_FOUND));

        member.setRole(request.getRole());
        memberRepository.save(member);

        log.info("Member role updated: workdspaceId={} userId={} newRole={}",
                workspaceId, userId, request.getRole());
    }

    @Override
    public List<WorkspaceMember> getMembers(Long workspaceId){
        findWorkspace(workspaceId);
        return memberRepository.findByWorkspaceId(workspaceId);
    }

    // ===== Helper methods =====

    private Workspace findWorkspace(Long workspaceId) {
        return workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new CustomException(
                        "Workspace not found", HttpStatus.NOT_FOUND));
    }

    private void requireMember(Long workspaceId, Long userId) {
        if (!memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw new CustomException("Access denied — you are not a member of this workspace",
                    HttpStatus.FORBIDDEN);
        }
    }

    private void requireAdmin(Long workspaceId, Long userId) {
        WorkspaceMember member = memberRepository
                .findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new CustomException(
                        "Access denied — you are not a member of this workspace", HttpStatus.FORBIDDEN));

        if (member.getRole() != MemberRole.ADMIN) {
            throw new CustomException("Access denied — admin role required", HttpStatus.FORBIDDEN);
        }
    }

    // Map entity → response DTO
    private WorkspaceResponse toResponse(Workspace workspace) {

        List<WorkspaceResponse.MemberDto> memberDtos = memberRepository
                .findByWorkspaceId(workspace.getId())
                .stream()
                .map(m -> WorkspaceResponse.MemberDto.builder()
                        .userId(m.getUserId())
                        .role(m.getRole())
                        .joinedAt(m.getJoinedAt())
                        .build())
                .toList();

        return WorkspaceResponse.builder()
                .id(workspace.getId())
                .name(workspace.getName())
                .description(workspace.getDescription())
                .ownerId(workspace.getOwnerId())
                .visibility(workspace.getVisibility())
                .logoUrl(workspace.getLogoUrl())
                .createdAt(workspace.getCreatedAt())
                .updatedAt(workspace.getUpdatedAt())
                .members(memberDtos)
                .build();
    }
}