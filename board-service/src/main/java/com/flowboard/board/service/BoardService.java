package com.flowboard.board.service;



import java.util.List;

import com.flowboard.board.dto.AddBoardMemberRequest;
import com.flowboard.board.dto.BoardResponse;
import com.flowboard.board.dto.CreateBoardRequest;
import com.flowboard.board.dto.UpdateBoardMemberRoleRequest;
import com.flowboard.board.dto.UpdateBoardRequest;
import com.flowboard.board.entity.BoardMember;

public interface BoardService {

    // Create board
    BoardResponse createBoard(CreateBoardRequest request, Long createdById);

    // Fetch single board
    BoardResponse getBoardById(Long workspace, Long requesterId);

    // Boards in a workspace
    List<BoardResponse> getBoardsByWorkspace(Long workspaceId, Long requesterId);

    // Boards where user is a member
    List<BoardResponse> getBoardsByMember(Long userId);

    // Boards created by user
    List<BoardResponse> getBoardsByCreator(Long createdById);

    // Public boards
    List<BoardResponse> getPublicBoards();

    // Closed boards in workspace
    List<BoardResponse> getClosedBoards(Long workspaceId, Long requesterId);

    // Update board
    BoardResponse updateBoard(Long boardId, UpdateBoardRequest request, Long requesterId);

    // Close / reopen board
    BoardResponse closeBoard(Long boardId, Long requesterId);
    BoardResponse reopenBoard(Long boardId, Long requesterId);

    // Delete board
    void deleteBoard(Long boardId, Long requesterId);

    // ===== Member management =====

    BoardMember addMember(Long boardId, AddBoardMemberRequest request, Long requesterId);

    void removeMember(Long boardId, Long userId, Long requesterId);

    void updateMemberRole(Long boardId, Long userId, UpdateBoardMemberRoleRequest request, Long requesterId);

    List<BoardMember> getMembers(Long boardId);

    // Board analytics
    BoardResponse.BoardAnalytics getBoardAnalytics(Long boardId, Long requesterId);
}