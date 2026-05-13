package com.flowboard.board.controller;

import com.flowboard.board.dto.AddBoardMemberRequest;
import com.flowboard.board.dto.BoardResponse;
import com.flowboard.board.dto.CreateBoardRequest;
import com.flowboard.board.dto.PublicBoardDetailResponse;
import com.flowboard.board.dto.UpdateBoardMemberRoleRequest;
import com.flowboard.board.dto.UpdateBoardRequest;
import com.flowboard.board.entity.BoardMember;
import com.flowboard.board.entity.BoardMemberRole;
import com.flowboard.board.exception.CustomException;
import com.flowboard.board.service.BoardService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardControllerTest {

    @Mock
    private BoardService boardService;

    private BoardController controller;
    private BoardResponse response;

    @BeforeEach
    void setUp() {
        controller = new BoardController(boardService);
        response = BoardResponse.builder().id(1L).name("Board").build();
    }

    @Test
    void boardEndpoints_ShouldDelegateToServiceAndReturnResponses() {
        CreateBoardRequest createRequest = new CreateBoardRequest();
        UpdateBoardRequest updateRequest = new UpdateBoardRequest();
        AddBoardMemberRequest addMemberRequest = new AddBoardMemberRequest();
        UpdateBoardMemberRoleRequest roleRequest = new UpdateBoardMemberRoleRequest();
        BoardMember member = BoardMember.builder().userId(2L).role(BoardMemberRole.MEMBER).build();
        PublicBoardDetailResponse publicDetail = PublicBoardDetailResponse.builder().id(1L).name("Board").build();
        BoardResponse.BoardAnalytics analytics = BoardResponse.BoardAnalytics.builder().totalMembers(1).build();

        when(boardService.createBoard(createRequest, 1L, true)).thenReturn(response);
        when(boardService.getBoardById(1L, 1L)).thenReturn(response);
        when(boardService.getBoardsByWorkspace(10L, 1L, false)).thenReturn(List.of(response));
        when(boardService.getBoardsByMember(2L)).thenReturn(List.of(response));
        when(boardService.getBoardsByCreator(1L)).thenReturn(List.of(response));
        when(boardService.getPublicBoards()).thenReturn(List.of(response));
        when(boardService.getPublicBoardDetail(1L)).thenReturn(publicDetail);
        when(boardService.getPublicBoardDetailsByWorkspace(10L)).thenReturn(List.of(publicDetail));
        when(boardService.getClosedBoards(10L, 1L)).thenReturn(List.of(response));
        when(boardService.updateBoard(1L, updateRequest, 1L)).thenReturn(response);
        when(boardService.closeBoard(1L, 1L)).thenReturn(response);
        when(boardService.reopenBoard(1L, 1L)).thenReturn(response);
        when(boardService.addMember(1L, addMemberRequest, 1L)).thenReturn(member);
        when(boardService.getMembers(1L)).thenReturn(List.of(member));
        when(boardService.getBoardAnalytics(1L, 1L)).thenReturn(analytics);

        assertEquals(HttpStatus.CREATED, controller.create(createRequest, 1L, "PREMIUM", "ACTIVE").getStatusCode());
        assertEquals(response, controller.getById(1L, 1L).getBody());
        assertEquals(1, controller.getByWorkspace(10L, 1L, "FREE", "EXPIRED").getBody().size());
        assertEquals(1, controller.getByMember(2L).getBody().size());
        assertEquals(1, controller.getByCreator(1L).getBody().size());
        assertEquals(1, controller.getPublic().getBody().size());
        assertEquals(publicDetail, controller.getPublicBoardDetail(1L).getBody());
        assertEquals(1, controller.getPublicWorkspaceBoardDetails(10L).getBody().size());
        assertEquals(1, controller.getClosedBoards(10L, 1L).getBody().size());
        assertEquals(response, controller.update(1L, updateRequest, 1L).getBody());
        assertEquals(response, controller.close(1L, 1L).getBody());
        assertEquals(response, controller.reopen(1L, 1L).getBody());
        assertEquals("Board removed successfully", controller.delete(1L, 1L).getBody());
        assertEquals(member, controller.addMember(1L, addMemberRequest, 1L).getBody());
        assertEquals("Member removed successfully", controller.deleteMember(1L, 2L, 1L).getBody());
        assertEquals("Member role updated", controller.updateMemberRole(1L, 2L, roleRequest, 1L).getBody());
        assertEquals(1, controller.getMembers(1L).getBody().size());
        assertEquals(analytics, controller.getAnalytics(1L, 1L).getBody());

        verify(boardService).deleteBoard(1L, 1L);
        verify(boardService).removeMember(1L, 2L, 1L);
        verify(boardService).updateMemberRole(1L, 2L, roleRequest, 1L);
    }

    @Test
    void create_WhenUserHeaderMissing_ShouldThrowBadRequest() {
        CreateBoardRequest request = new CreateBoardRequest();

        CustomException exception = assertThrows(
                CustomException.class,
                () -> controller.create(request, null, "PREMIUM", "ACTIVE")
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void create_WhenSubscriptionInactive_ShouldPassFreePlan() {
        CreateBoardRequest request = new CreateBoardRequest();
        when(boardService.createBoard(eq(request), eq(1L), eq(false))).thenReturn(response);

        controller.create(request, 1L, "PREMIUM", "EXPIRED");

        verify(boardService).createBoard(any(CreateBoardRequest.class), eq(1L), eq(false));
    }
}
