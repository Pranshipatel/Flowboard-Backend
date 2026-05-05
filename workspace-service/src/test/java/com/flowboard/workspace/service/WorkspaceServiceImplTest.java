package com.flowboard.workspace.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import com.flowboard.workspace.dto.CreateWorkspaceRequest;
import com.flowboard.workspace.dto.WorkspaceResponse;
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
}
