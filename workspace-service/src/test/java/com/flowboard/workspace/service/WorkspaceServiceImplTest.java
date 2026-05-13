package com.flowboard.workspace.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import com.flowboard.workspace.dto.AddMemberRequest;
import com.flowboard.workspace.dto.CreateWorkspaceRequest;
import com.flowboard.workspace.dto.UpdateMemberRoleRequest;
import com.flowboard.workspace.dto.UpdateWorkspaceRequest;
import com.flowboard.workspace.dto.WorkspaceMemberResponse;
import com.flowboard.workspace.dto.WorkspaceResponse;
import com.flowboard.workspace.entity.MemberRole;
import com.flowboard.workspace.entity.Visibility;
import com.flowboard.workspace.entity.Workspace;
import com.flowboard.workspace.entity.WorkspaceMember;
import com.flowboard.workspace.exception.CustomException;
import com.flowboard.workspace.repository.WorkspaceMemberRepository;
import com.flowboard.workspace.repository.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceImplTest {

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceMemberRepository memberRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private WorkspaceServiceImpl workspaceService;

    private Workspace testWorkspace;
    private CreateWorkspaceRequest createRequest;

    @BeforeEach
    void setUp() {
        testWorkspace = Workspace.builder()
                .id(1L)
                .name("Test Workspace")
                .ownerId(1L)
                .visibility(Visibility.PRIVATE)
                .createdAt(LocalDateTime.now())
                .build();

        createRequest = new CreateWorkspaceRequest();
        createRequest.setName("Test Workspace");
        createRequest.setVisibility(Visibility.PRIVATE);
    }

    @Test
    void createWorkspace_WhenValid_ShouldReturnWorkspace() {
        when(workspaceRepository.existsByNameAndOwnerId("Test Workspace", 1L)).thenReturn(false);
        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(i -> {
            Workspace w = i.getArgument(0);
            w.setId(1L);
            return w;
        });

        WorkspaceResponse response = workspaceService.createWorkspace(createRequest, 1L);

        assertNotNull(response);
        assertEquals("Test Workspace", response.getName());
        verify(workspaceRepository).save(any(Workspace.class));
        verify(memberRepository).save(any(WorkspaceMember.class));
    }

    @Test
    void createWorkspace_WhenExists_ShouldThrowException() {
        when(workspaceRepository.existsByNameAndOwnerId("Test Workspace", 1L)).thenReturn(true);

        assertThrows(CustomException.class, () -> workspaceService.createWorkspace(createRequest, 1L));
    }

    @Test
    void getById_WhenPublic_ShouldReturn() {
        testWorkspace.setVisibility(Visibility.PUBLIC);
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(testWorkspace));

        WorkspaceResponse response = workspaceService.getById(1L, 2L);

        assertNotNull(response);
        assertEquals("Test Workspace", response.getName());
    }

    @Test
    void getById_WhenPrivateAndNotMember_ShouldThrowException() {
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(testWorkspace));
        when(memberRepository.existsByWorkspaceIdAndUserId(1L, 2L)).thenReturn(false);

        assertThrows(CustomException.class, () -> workspaceService.getById(1L, 2L));
    }

    @Test
    void getById_WhenPrivateAndMember_ShouldReturn() {
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(testWorkspace));
        when(memberRepository.existsByWorkspaceIdAndUserId(1L, 1L)).thenReturn(true);

        WorkspaceResponse response = workspaceService.getById(1L, 1L);

        assertNotNull(response);
    }

    @Test
    void listQueries_ShouldMapWorkspaceResponses() {
        testWorkspace.setVisibility(Visibility.PUBLIC);
        when(workspaceRepository.findByOwnerId(1L)).thenReturn(List.of(testWorkspace));
        when(workspaceRepository.findByMemberUserId(2L)).thenReturn(List.of(testWorkspace));
        when(workspaceRepository.findByVisibility(Visibility.PUBLIC)).thenReturn(List.of(testWorkspace));
        when(memberRepository.findByWorkspaceId(1L)).thenReturn(List.of(member(10L, 1L, MemberRole.ADMIN)));

        assertEquals(1, workspaceService.getByOwner(1L).size());
        assertEquals(1, workspaceService.getByMember(2L).size());
        assertEquals(1, workspaceService.getPublicWorkspaces().size());
    }

    @Test
    void updateWorkspace_WhenAdmin_ShouldPersistChanges() {
        UpdateWorkspaceRequest request = new UpdateWorkspaceRequest();
        request.setName("Updated");
        request.setDescription("New description");
        request.setVisibility(Visibility.PUBLIC);
        request.setLogoUrl("logo.png");

        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(testWorkspace));
        when(memberRepository.findByWorkspaceIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(member(11L, 1L, MemberRole.ADMIN)));
        when(memberRepository.findByWorkspaceId(1L)).thenReturn(List.of(member(11L, 1L, MemberRole.ADMIN)));

        WorkspaceResponse response = workspaceService.updateWorkspace(1L, request, 1L);

        assertEquals("Updated", response.getName());
        assertEquals(Visibility.PUBLIC, response.getVisibility());
        verify(workspaceRepository).save(testWorkspace);
    }

    @Test
    void addMember_WhenAdmin_ShouldSaveMemberAndNotify() {
        AddMemberRequest request = new AddMemberRequest();
        request.setUserId(2L);
        request.setRole(MemberRole.MEMBER);

        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(testWorkspace));
        when(memberRepository.findByWorkspaceIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(member(11L, 1L, MemberRole.ADMIN)));
        when(memberRepository.existsByWorkspaceIdAndUserId(1L, 2L)).thenReturn(false);
        when(memberRepository.save(any(WorkspaceMember.class))).thenAnswer(i -> {
            WorkspaceMember m = i.getArgument(0);
            m.setId(22L);
            return m;
        });

        WorkspaceMemberResponse response = workspaceService.addMember(1L, request, 1L);

        assertEquals(2L, response.getUserId());
        assertEquals("MEMBER", response.getRole());
        verify(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(Object.class));
    }

    @Test
    void addMember_WhenAlreadyMember_ShouldThrowException() {
        AddMemberRequest request = new AddMemberRequest();
        request.setUserId(2L);

        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(testWorkspace));
        when(memberRepository.findByWorkspaceIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(member(11L, 1L, MemberRole.ADMIN)));
        when(memberRepository.existsByWorkspaceIdAndUserId(1L, 2L)).thenReturn(true);

        assertThrows(CustomException.class, () -> workspaceService.addMember(1L, request, 1L));
    }

    @Test
    void removeMember_WhenAdmin_ShouldDeleteAndNotify() {
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(testWorkspace));
        when(memberRepository.findByWorkspaceIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(member(11L, 1L, MemberRole.ADMIN)));
        when(memberRepository.existsByWorkspaceIdAndUserId(1L, 2L)).thenReturn(true);

        workspaceService.removeMember(1L, 2L, 1L);

        verify(memberRepository).deleteByWorkspaceIdAndUserId(1L, 2L);
        verify(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(Object.class));
    }

    @Test
    void removeMember_WhenTargetIsOwner_ShouldThrowException() {
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(testWorkspace));
        when(memberRepository.findByWorkspaceIdAndUserId(1L, 2L))
                .thenReturn(Optional.of(member(12L, 2L, MemberRole.ADMIN)));
        when(memberRepository.existsByWorkspaceIdAndUserId(1L, 1L)).thenReturn(true);

        assertThrows(CustomException.class, () -> workspaceService.removeMember(1L, 1L, 2L));
    }

    @Test
    void updateMemberRole_WhenMemberExists_ShouldSaveRole() {
        UpdateMemberRoleRequest request = new UpdateMemberRoleRequest();
        request.setRole(MemberRole.ADMIN);
        WorkspaceMember target = member(22L, 2L, MemberRole.MEMBER);

        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(testWorkspace));
        when(memberRepository.findByWorkspaceIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(member(11L, 1L, MemberRole.ADMIN)));
        when(memberRepository.findByWorkspaceIdAndUserId(1L, 2L)).thenReturn(Optional.of(target));

        workspaceService.updateMemberRole(1L, 2L, request, 1L);

        assertEquals(MemberRole.ADMIN, target.getRole());
        verify(memberRepository).save(target);
    }

    @Test
    void getMembers_ShouldReturnMemberDtos() {
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(testWorkspace));
        when(memberRepository.findByWorkspaceId(1L))
                .thenReturn(List.of(member(11L, 1L, MemberRole.ADMIN), member(12L, 2L, MemberRole.MEMBER)));

        List<WorkspaceMemberResponse> response = workspaceService.getMembers(1L);

        assertEquals(2, response.size());
        assertEquals("ADMIN", response.get(0).getRole());
    }

    @Test
    void deleteWorkspace_WhenNotOwner_ShouldThrowException() {
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(testWorkspace));

        assertThrows(CustomException.class, () -> workspaceService.deleteWorkspace(1L, 2L));
    }

    @Test
    void deleteWorkspace_WhenOwner_ShouldDelete() {
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(testWorkspace));

        workspaceService.deleteWorkspace(1L, 1L);

        verify(workspaceRepository).delete(testWorkspace);
    }

    private WorkspaceMember member(Long id, Long userId, MemberRole role) {
        return WorkspaceMember.builder()
                .id(id)
                .workspace(testWorkspace)
                .userId(userId)
                .role(role)
                .joinedAt(LocalDateTime.now())
                .build();
    }
}
