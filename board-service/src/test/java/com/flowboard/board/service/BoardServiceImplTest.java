package com.flowboard.board.service;

import com.flowboard.board.client.CardClient;
import com.flowboard.board.client.ListClient;
import com.flowboard.board.client.WorkspaceClient;
import com.flowboard.board.dto.AddBoardMemberRequest;
import com.flowboard.board.dto.BoardResponse;
import com.flowboard.board.dto.CreateBoardRequest;
import com.flowboard.board.dto.PublicCardClientResponse;
import com.flowboard.board.dto.PublicListClientResponse;
import com.flowboard.board.dto.UpdateBoardMemberRoleRequest;
import com.flowboard.board.dto.UpdateBoardRequest;
import com.flowboard.board.dto.WorkspaceMemberResponse;
import com.flowboard.board.dto.WorkspaceResponse;
import com.flowboard.board.entity.Board;
import com.flowboard.board.entity.BoardMember;
import com.flowboard.board.entity.BoardMemberRole;
import com.flowboard.board.entity.Visibility;
import com.flowboard.board.exception.CustomException;
import com.flowboard.board.repository.BoardMemberRepository;
import com.flowboard.board.repository.BoardRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardServiceImplTest {

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private BoardMemberRepository memberRepository;

    @Mock
    private WorkspaceClient workspaceClient;

    @Mock
    private CardClient cardClient;

    @Mock
    private ListClient listClient;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private BoardServiceImpl boardService;

    private Board board;
    private BoardMember adminMember;

    @BeforeEach
    void setUp() {
        board = board(1L, 10L, "Project Alpha", Visibility.PRIVATE, 1L, false);
        adminMember = member(1L, BoardMemberRole.ADMIN);
    }

    @Test
    void createBoard_WhenPremiumPublicWorkspace_ShouldCreateDefaultsAndNotifyMembers() {
        CreateBoardRequest request = createRequest(10L, "Project Alpha", Visibility.PUBLIC);
        when(workspaceClient.getWorkspaceById(10L, 1L)).thenReturn(workspace(10L, 1L, "PUBLIC"));
        when(boardRepository.save(any(Board.class))).thenAnswer(invocation -> {
            Board saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        when(workspaceClient.getMembers(10L, 1L)).thenReturn(List.of(
                workspaceMember(1L),
                workspaceMember(2L)
        ));

        BoardResponse response = boardService.createBoard(request, 1L, true);

        assertEquals(1L, response.getId());
        assertEquals(Visibility.PUBLIC, response.getVisibility());
        verify(memberRepository).save(any(BoardMember.class));
        verify(listClient, times(4)).createList(any(), eq(1L), eq("PREMIUM"), eq("ACTIVE"));
        verify(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(Object.class));
    }

    @Test
    void createBoard_WhenFreeLimitReached_ShouldThrow() {
        CreateBoardRequest request = createRequest(10L, "Extra", Visibility.PUBLIC);
        when(workspaceClient.getWorkspaceById(10L, 1L)).thenReturn(workspace(10L, 1L, "PUBLIC"));
        when(boardRepository.countByWorkspaceId(10L)).thenReturn(2L);

        assertThrows(CustomException.class, () -> boardService.createBoard(request, 1L, false));

        verifyNoInteractions(listClient, rabbitTemplate);
    }

    @Test
    void createBoard_WhenRequesterIsNotWorkspaceOwner_ShouldThrow() {
        CreateBoardRequest request = createRequest(10L, "Other Owner", Visibility.PUBLIC);
        when(workspaceClient.getWorkspaceById(10L, 2L)).thenReturn(workspace(10L, 1L, "PUBLIC"));

        assertThrows(CustomException.class, () -> boardService.createBoard(request, 2L, true));

        verifyNoInteractions(boardRepository, memberRepository, listClient, rabbitTemplate);
    }

    @Test
    void createBoard_WhenVisibilityMissing_ShouldDefaultPrivate() {
        CreateBoardRequest request = createRequest(10L, "No Visibility", null);
        when(workspaceClient.getWorkspaceById(10L, 1L)).thenReturn(workspace(10L, 1L, "PUBLIC"));
        when(boardRepository.save(any(Board.class))).thenAnswer(invocation -> {
            Board saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        when(workspaceClient.getMembers(10L, 1L)).thenReturn(List.of(workspaceMember(1L)));

        BoardResponse response = boardService.createBoard(request, 1L, false);

        assertEquals(Visibility.PRIVATE, response.getVisibility());
        verify(listClient, times(2)).createList(any(), eq(1L), eq("FREE"), eq("EXPIRED"));
        verify(rabbitTemplate, never()).convertAndSend(any(String.class), any(String.class), any(Object.class));
    }

    @Test
    void createBoard_WhenWorkspaceIsPrivate_ShouldForceBoardPrivateAndIgnoreSideEffectFailures() {
        CreateBoardRequest request = createRequest(10L, "Private Board", Visibility.PUBLIC);
        when(workspaceClient.getWorkspaceById(10L, 1L)).thenReturn(workspace(10L, 1L, "PRIVATE"));
        when(boardRepository.save(any(Board.class))).thenAnswer(invocation -> {
            Board saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        doThrow(new RuntimeException("list service down")).when(listClient)
                .createList(any(), eq(1L), eq("FREE"), eq("EXPIRED"));
        when(workspaceClient.getMembers(10L, 1L)).thenThrow(new RuntimeException("workspace service down"));

        BoardResponse response = boardService.createBoard(request, 1L, false);

        assertEquals(Visibility.PRIVATE, response.getVisibility());
    }

    @Test
    void getBoardById_WhenPrivateAndRequesterIsNotParticipant_ShouldThrow() {
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));
        when(workspaceClient.getWorkspaceById(10L, 2L)).thenReturn(workspace(10L, 1L, "PRIVATE"));
        when(workspaceClient.getMembers(10L, 2L)).thenReturn(List.of());

        assertThrows(CustomException.class, () -> boardService.getBoardById(1L, 2L));
    }

    @Test
    void getBoardById_WhenPrivateAndRequesterIsWorkspaceMember_ShouldReturnBoard() {
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));
        when(workspaceClient.getWorkspaceById(10L, 2L)).thenReturn(workspace(10L, 1L, "PRIVATE"));
        when(workspaceClient.getMembers(10L, 2L)).thenReturn(List.of(workspaceMember(2L)));

        BoardResponse response = boardService.getBoardById(1L, 2L);

        assertEquals(1L, response.getId());
    }

    @Test
    void getBoardsByWorkspace_WhenParticipantPremium_ShouldIncludeStats() {
        Board second = board(2L, 10L, "Project Beta", Visibility.PUBLIC, 1L, false);
        when(workspaceClient.getWorkspaceById(10L, 1L)).thenReturn(workspace(10L, 1L, "PUBLIC"));
        when(boardRepository.findByWorkspaceId(10L)).thenReturn(List.of(board, second));
        when(cardClient.getBoardStats(anyList())).thenReturn(List.of(
                new CardClient.BoardStatsResponse(1L, 5, 2),
                new CardClient.BoardStatsResponse(2L, 4, 4)
        ));

        List<BoardResponse> responses = boardService.getBoardsByWorkspace(10L, 1L, true);

        assertEquals(2, responses.size());
        assertEquals(40, responses.get(0).getProgressPercentage());
        assertEquals(100, responses.get(1).getProgressPercentage());
    }

    @Test
    void getBoardsByWorkspace_WhenNotParticipantFree_ShouldOnlyReturnPublicBoards() {
        Board publicBoard = board(2L, 10L, "Public Board", Visibility.PUBLIC, 1L, false);
        when(workspaceClient.getWorkspaceById(10L, 7L)).thenReturn(workspace(10L, 1L, "PUBLIC"));
        when(workspaceClient.getMembers(10L, 7L)).thenReturn(List.of());
        when(boardRepository.findByWorkspaceId(10L)).thenReturn(List.of(board, publicBoard));
        when(cardClient.getBoardStats(anyList())).thenThrow(new RuntimeException("card service down"));

        List<BoardResponse> responses = boardService.getBoardsByWorkspace(10L, 7L, false);

        assertEquals(1, responses.size());
        assertEquals("Public Board", responses.get(0).getName());
        assertEquals(0, responses.get(0).getProgressPercentage());
    }

    @Test
    void listQueries_ShouldMapRepositoryResults() {
        when(boardRepository.findByMemberUserId(1L)).thenReturn(List.of(board));
        when(boardRepository.findByCreatedById(1L)).thenReturn(List.of(board));
        when(boardRepository.findByVisibility(Visibility.PUBLIC)).thenReturn(List.of(board));

        assertEquals(1, boardService.getBoardsByMember(1L).size());
        assertEquals(1, boardService.getBoardsByCreator(1L).size());
        assertEquals(1, boardService.getPublicBoards().size());
    }

    @Test
    void listQueries_WhenRepositoryReturnsEmpty_ShouldReturnEmptyWithoutStatsCall() {
        when(boardRepository.findByMemberUserId(1L)).thenReturn(List.of());

        List<BoardResponse> responses = boardService.getBoardsByMember(1L);

        assertTrue(responses.isEmpty());
        verifyNoInteractions(cardClient);
    }

    @Test
    void publicBoardDetail_WhenPublicAndWorkspacePublic_ShouldAttachListsAndCards() {
        board.setVisibility(Visibility.PUBLIC);
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));
        when(workspaceClient.getPublicWorkspaceById(10L)).thenReturn(workspace(10L, 1L, "PUBLIC"));
        when(listClient.getByBoard(1L, "PREMIUM", "ACTIVE")).thenReturn(List.of(publicList(100L)));
        when(cardClient.getByBoard(1L, "PREMIUM", "ACTIVE")).thenReturn(List.of(publicCard(200L, 100L)));

        var response = boardService.getPublicBoardDetail(1L);

        assertEquals(1L, response.getId());
        assertEquals(1, response.getLists().size());
        assertEquals(1, response.getLists().get(0).getCards().size());
    }

    @Test
    void publicBoardDetail_WhenClosedOrPrivate_ShouldThrow() {
        board.setClosed(true);
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));

        assertThrows(CustomException.class, () -> boardService.getPublicBoardDetail(1L));
    }

    @Test
    void publicBoardDetail_WhenWorkspaceIsPrivate_ShouldThrow() {
        board.setVisibility(Visibility.PUBLIC);
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));
        when(workspaceClient.getPublicWorkspaceById(10L)).thenReturn(workspace(10L, 1L, "PRIVATE"));

        assertThrows(CustomException.class, () -> boardService.getPublicBoardDetail(1L));
    }

    @Test
    void publicBoardDetailsByWorkspace_WhenWorkspacePublic_ShouldReturnOpenPublicBoards() {
        Board closedPublic = board(2L, 10L, "Closed", Visibility.PUBLIC, 1L, true);
        Board openPublic = board(3L, 10L, "Open", Visibility.PUBLIC, 1L, false);
        when(workspaceClient.getPublicWorkspaceById(10L)).thenReturn(workspace(10L, 1L, "PUBLIC"));
        when(boardRepository.findByWorkspaceId(10L)).thenReturn(List.of(board, closedPublic, openPublic));

        var responses = boardService.getPublicBoardDetailsByWorkspace(10L);

        assertEquals(1, responses.size());
        assertEquals("Open", responses.get(0).getName());
    }

    @Test
    void publicBoardDetailsByWorkspace_WhenWorkspacePrivate_ShouldThrow() {
        when(workspaceClient.getPublicWorkspaceById(10L)).thenReturn(workspace(10L, 1L, null));

        assertThrows(CustomException.class, () -> boardService.getPublicBoardDetailsByWorkspace(10L));
    }

    @Test
    void getClosedBoards_ShouldOnlyReturnClosedBoardsWhereRequesterIsMember() {
        Board closedOne = board(1L, 10L, "Closed One", Visibility.PRIVATE, 1L, true);
        Board closedTwo = board(2L, 10L, "Closed Two", Visibility.PUBLIC, 1L, true);
        when(boardRepository.findByWorkspaceIdAndIsClosed(10L, true)).thenReturn(List.of(closedOne, closedTwo));
        when(memberRepository.existsByBoardIdAndUserId(1L, 2L)).thenReturn(true);
        when(memberRepository.existsByBoardIdAndUserId(2L, 2L)).thenReturn(false);

        List<BoardResponse> responses = boardService.getClosedBoards(10L, 2L);

        assertEquals(1, responses.size());
        assertEquals("Closed One", responses.get(0).getName());
    }

    @Test
    void updateBoard_WhenAdmin_ShouldUpdateMutableFields() {
        UpdateBoardRequest request = new UpdateBoardRequest();
        request.setName("Updated");
        request.setDescription("New description");
        request.setBackground("green");
        request.setVisibility(Visibility.PUBLIC);
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));
        when(memberRepository.findByBoardIdAndUserId(1L, 1L)).thenReturn(Optional.of(adminMember));
        when(workspaceClient.getWorkspaceById(10L, 1L)).thenReturn(workspace(10L, 1L, "PUBLIC"));

        BoardResponse response = boardService.updateBoard(1L, request, 1L);

        assertEquals("Updated", response.getName());
        assertEquals(Visibility.PUBLIC, response.getVisibility());
        verify(boardRepository).save(board);
    }

    @Test
    void updateBoard_WhenWorkspacePrivate_ShouldForceVisibilityPrivateAndKeepBackground() {
        UpdateBoardRequest request = new UpdateBoardRequest();
        request.setName("Updated");
        request.setDescription("New description");
        request.setBackground(null);
        request.setVisibility(Visibility.PUBLIC);
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));
        when(memberRepository.findByBoardIdAndUserId(1L, 1L)).thenReturn(Optional.of(adminMember));
        when(workspaceClient.getWorkspaceById(10L, 1L)).thenReturn(workspace(10L, 1L, "PRIVATE"));

        BoardResponse response = boardService.updateBoard(1L, request, 1L);

        assertEquals(Visibility.PRIVATE, response.getVisibility());
        assertEquals("blue", response.getBackground());
    }

    @Test
    void updateBoard_WhenClosed_ShouldThrow() {
        board.setClosed(true);
        UpdateBoardRequest request = new UpdateBoardRequest();
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));
        when(memberRepository.findByBoardIdAndUserId(1L, 1L)).thenReturn(Optional.of(adminMember));

        assertThrows(CustomException.class, () -> boardService.updateBoard(1L, request, 1L));
    }

    @Test
    void closeAndReopenBoard_ShouldUpdateClosedState() {
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));
        when(memberRepository.findByBoardIdAndUserId(1L, 1L)).thenReturn(Optional.of(adminMember));

        assertTrue(boardService.closeBoard(1L, 1L).isClosed());
        assertFalse(boardService.reopenBoard(1L, 1L).isClosed());
    }

    @Test
    void closeAndReopenBoard_WhenStateInvalid_ShouldThrow() {
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));
        when(memberRepository.findByBoardIdAndUserId(1L, 1L)).thenReturn(Optional.of(adminMember));

        assertThrows(CustomException.class, () -> boardService.reopenBoard(1L, 1L));

        board.setClosed(true);
        assertThrows(CustomException.class, () -> boardService.closeBoard(1L, 1L));
    }

    @Test
    void deleteBoard_WhenAdminButNotCreator_ShouldDeleteAndNotify() {
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));
        when(memberRepository.findByBoardIdAndUserId(1L, 2L)).thenReturn(Optional.of(member(2L, BoardMemberRole.ADMIN)));
        when(workspaceClient.getWorkspaceById(10L, 2L)).thenReturn(workspace(10L, 1L, "PUBLIC"));
        when(workspaceClient.getMembers(10L, 2L)).thenReturn(List.of(workspaceMember(1L), workspaceMember(2L)));

        boardService.deleteBoard(1L, 2L);

        verify(boardRepository).delete(board);
        verify(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(Object.class));
    }

    @Test
    void deleteBoard_WhenCreatorAndNotificationFails_ShouldStillDelete() {
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));
        when(workspaceClient.getWorkspaceById(10L, 1L)).thenThrow(new RuntimeException("workspace down"));

        boardService.deleteBoard(1L, 1L);

        verify(boardRepository).delete(board);
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void deleteBoard_WhenRequesterIsNotCreatorOrAdmin_ShouldThrow() {
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));
        when(memberRepository.findByBoardIdAndUserId(1L, 2L)).thenReturn(Optional.of(member(2L, BoardMemberRole.MEMBER)));

        assertThrows(CustomException.class, () -> boardService.deleteBoard(1L, 2L));
    }

    @Test
    void memberManagement_ShouldAddRemoveUpdateAndReportAnalytics() {
        AddBoardMemberRequest addRequest = new AddBoardMemberRequest();
        addRequest.setUserId(3L);
        addRequest.setRole(null);
        UpdateBoardMemberRoleRequest roleRequest = new UpdateBoardMemberRoleRequest();
        roleRequest.setRole(BoardMemberRole.OBSERVER);
        BoardMember member = member(3L, BoardMemberRole.MEMBER);
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));
        when(memberRepository.findByBoardIdAndUserId(1L, 1L)).thenReturn(Optional.of(adminMember));
        when(memberRepository.existsByBoardIdAndUserId(1L, 3L)).thenReturn(false, true);
        when(memberRepository.save(any(BoardMember.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(memberRepository.findByBoardIdAndUserId(1L, 3L)).thenReturn(Optional.of(member));
        when(memberRepository.findByBoardId(1L)).thenReturn(List.of(adminMember, member(2L, BoardMemberRole.OBSERVER), member));

        BoardMember added = boardService.addMember(1L, addRequest, 1L);
        boardService.updateMemberRole(1L, 3L, roleRequest, 1L);
        boardService.removeMember(1L, 3L, 1L);
        List<BoardMember> members = boardService.getMembers(1L);
        BoardResponse.BoardAnalytics analytics = boardService.getBoardAnalytics(1L, 3L);

        assertEquals(BoardMemberRole.MEMBER, added.getRole());
        assertEquals(BoardMemberRole.OBSERVER, member.getRole());
        assertEquals(3, members.size());
        assertEquals(3, analytics.getTotalMembers());
        verify(memberRepository).deleteByBoardIdAndUserId(1L, 3L);
    }

    @Test
    void addMember_WhenExplicitRoleProvided_ShouldUseRequestedRole() {
        AddBoardMemberRequest addRequest = new AddBoardMemberRequest();
        addRequest.setUserId(4L);
        addRequest.setRole(BoardMemberRole.OBSERVER);
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));
        when(memberRepository.findByBoardIdAndUserId(1L, 1L)).thenReturn(Optional.of(adminMember));
        when(memberRepository.existsByBoardIdAndUserId(1L, 4L)).thenReturn(false);
        when(memberRepository.save(any(BoardMember.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BoardMember added = boardService.addMember(1L, addRequest, 1L);

        assertEquals(BoardMemberRole.OBSERVER, added.getRole());
    }

    @Test
    void memberManagement_WhenInvalid_ShouldThrow() {
        AddBoardMemberRequest addRequest = new AddBoardMemberRequest();
        addRequest.setUserId(3L);
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));
        when(memberRepository.findByBoardIdAndUserId(1L, 1L)).thenReturn(Optional.of(adminMember));
        when(memberRepository.existsByBoardIdAndUserId(1L, 3L)).thenReturn(true);

        assertThrows(CustomException.class, () -> boardService.addMember(1L, addRequest, 1L));
        assertThrows(CustomException.class, () -> boardService.removeMember(1L, 1L, 1L));
    }

    @Test
    void memberManagement_WhenBoardClosedOrMissingMember_ShouldThrow() {
        AddBoardMemberRequest addRequest = new AddBoardMemberRequest();
        addRequest.setUserId(3L);
        UpdateBoardMemberRoleRequest roleRequest = new UpdateBoardMemberRoleRequest();
        roleRequest.setRole(BoardMemberRole.ADMIN);
        board.setClosed(true);
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));
        when(memberRepository.findByBoardIdAndUserId(1L, 1L)).thenReturn(Optional.of(adminMember));

        assertThrows(CustomException.class, () -> boardService.addMember(1L, addRequest, 1L));

        board.setClosed(false);
        when(memberRepository.existsByBoardIdAndUserId(1L, 3L)).thenReturn(false);
        assertThrows(CustomException.class, () -> boardService.removeMember(1L, 3L, 1L));

        when(memberRepository.findByBoardIdAndUserId(1L, 3L)).thenReturn(Optional.empty());
        assertThrows(CustomException.class, () -> boardService.updateMemberRole(1L, 3L, roleRequest, 1L));
    }

    @Test
    void protectedActions_WhenRequesterIsNotMemberOrNotAdmin_ShouldThrow() {
        AddBoardMemberRequest addRequest = new AddBoardMemberRequest();
        addRequest.setUserId(3L);
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));
        when(memberRepository.findByBoardIdAndUserId(1L, 2L))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(member(2L, BoardMemberRole.MEMBER)));

        assertThrows(CustomException.class, () -> boardService.addMember(1L, addRequest, 2L));
        assertThrows(CustomException.class, () -> boardService.closeBoard(1L, 2L));
    }

    @Test
    void getBoardAnalytics_WhenRequesterIsNotMember_ShouldThrow() {
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));
        when(memberRepository.existsByBoardIdAndUserId(1L, 2L)).thenReturn(false);

        assertThrows(CustomException.class, () -> boardService.getBoardAnalytics(1L, 2L));
    }

    @Test
    void findBoard_WhenMissing_ShouldThrow() {
        when(boardRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(CustomException.class, () -> boardService.getMembers(99L));
    }

    private static Board board(Long id, Long workspaceId, String name, Visibility visibility, Long creatorId, boolean closed) {
        return Board.builder()
                .id(id)
                .workspaceId(workspaceId)
                .name(name)
                .description("Description")
                .background("blue")
                .visibility(visibility)
                .createdById(creatorId)
                .isClosed(closed)
                .createdAt(LocalDateTime.now().minusDays(1))
                .dueDate(LocalDateTime.now().plusDays(1))
                .build();
    }

    private static BoardMember member(Long userId, BoardMemberRole role) {
        return BoardMember.builder()
                .userId(userId)
                .role(role)
                .addedAt(LocalDateTime.now())
                .build();
    }

    private static CreateBoardRequest createRequest(Long workspaceId, String name, Visibility visibility) {
        CreateBoardRequest request = new CreateBoardRequest();
        request.setWorkspaceId(workspaceId);
        request.setName(name);
        request.setDescription("Description");
        request.setBackground("blue");
        request.setVisibility(visibility);
        return request;
    }

    private static WorkspaceResponse workspace(Long id, Long ownerId, String visibility) {
        return WorkspaceResponse.builder()
                .id(id)
                .ownerId(ownerId)
                .name("Workspace")
                .visibility(visibility)
                .build();
    }

    private static WorkspaceMemberResponse workspaceMember(Long userId) {
        return WorkspaceMemberResponse.builder()
                .workspaceId(10L)
                .userId(userId)
                .role("MEMBER")
                .build();
    }

    private static PublicListClientResponse publicList(Long id) {
        PublicListClientResponse list = new PublicListClientResponse();
        list.setId(id);
        list.setBoardId(1L);
        list.setName("To Do");
        list.setPosition(0);
        list.setColor("#fff");
        return list;
    }

    private static PublicCardClientResponse publicCard(Long id, Long listId) {
        PublicCardClientResponse card = new PublicCardClientResponse();
        card.setId(id);
        card.setListId(listId);
        card.setBoardId(1L);
        card.setTitle("Card");
        card.setDescription("Card description");
        card.setPosition(0);
        card.setPriority("HIGH");
        card.setStatus("TO_DO");
        card.setStartDate(LocalDate.now());
        card.setDueDate(LocalDate.now().plusDays(1));
        card.setOverdue(false);
        card.setCoverColor("#000");
        return card;
    }
}
