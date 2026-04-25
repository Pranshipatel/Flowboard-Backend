package com.flowboard.board.service;

import org.springframework.stereotype.Service;

import com.flowboard.board.dto.AddBoardMemberRequest;
import com.flowboard.board.dto.BoardResponse;
import com.flowboard.board.dto.CreateBoardRequest;
import com.flowboard.board.dto.UpdateBoardMemberRoleRequest;
import com.flowboard.board.dto.UpdateBoardRequest;
import com.flowboard.board.entity.Board;
import com.flowboard.board.entity.BoardMember;
import com.flowboard.board.entity.BoardMemberRole;
import com.flowboard.board.entity.Visibility;
import com.flowboard.board.exception.CustomException;
import com.flowboard.board.repository.BoardMemberRepository;
import com.flowboard.board.repository.BoardRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class BoardServiceImpl implements BoardService {

    private final BoardRepository boardRepository;
    private final BoardMemberRepository memberRepository;

    // ===== Board CRUD =====

    @Override
    @Transactional
    public BoardResponse createBoard(CreateBoardRequest request, Long createdById) {

        Board board = Board.builder()
                .workspaceId(request.getWorkspaceId())
                .name(request.getName())
                .description(request.getDescription())
                .background(request.getBackground())
                .visibility(request.getVisibility() != null ? request.getVisibility() : Visibility.PRIVATE)
                .createdById(createdById)
                .isClosed(false)
                .createdAt(LocalDateTime.now())
                .build();

        boardRepository.save(board);

        // Add creator as ADMIN
        BoardMember creatorMember = BoardMember.builder()
                .board(board)
                .userId(createdById)
                .role(BoardMemberRole.ADMIN)
                .addedAt(LocalDateTime.now())
                .build();

        memberRepository.save(creatorMember);

        log.info("Board created: id={} name={} workspaceId={} createdBy={}",
                board.getId(), board.getName(), board.getWorkspaceId(), createdById);

        return toResponse(board);
    }

    @Override
    public BoardResponse getBoardById(Long boardId, Long requesterId) {
        Board board = findBoard(boardId);

        if (board.getVisibility() == Visibility.PRIVATE) {
            requireMember(boardId, requesterId);
        }

        return toResponse(board);
    }

    @Override
    public List<BoardResponse> getBoardsByWorkspace(Long workspaceId, Long requesterId) {
        return boardRepository.findByWorkspaceId(workspaceId)
                .stream()
                .filter(b -> b.getVisibility() == Visibility.PUBLIC
                        || memberRepository.existsByBoardIdAndUserId(b.getId(), requesterId))
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<BoardResponse> getBoardsByMember(Long userId) {
        return boardRepository.findByMemberUserId(userId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public List<BoardResponse> getBoardsByCreator(Long createdById) {
        return boardRepository.findByCreatedById(createdById)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public List<BoardResponse> getPublicBoards() {
        return boardRepository.findByVisibility(Visibility.PUBLIC)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public List<BoardResponse> getClosedBoards(Long workspaceId, Long requesterId) {
        return boardRepository.findByWorkspaceIdAndIsClosed(workspaceId, true)
                .stream()
                .filter(b -> memberRepository.existsByBoardIdAndUserId(b.getId(), requesterId))
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public BoardResponse updateBoard(Long boardId, UpdateBoardRequest request, Long requesterId) {
        Board board = findBoard(boardId);
        requireAdmin(boardId, requesterId);

        if (board.isClosed()) {
            throw new CustomException("Board is closed. Reopen it before making changes",
                    HttpStatus.BAD_REQUEST);
        }

        board.setName(request.getName());
        board.setDescription(request.getDescription());
        if (request.getBackground() != null) board.setBackground(request.getBackground());
        if (request.getVisibility() != null) board.setVisibility(request.getVisibility());
        board.setUpdatedAt(LocalDateTime.now());

        boardRepository.save(board);
        log.info("Board updated: id={}", boardId);

        return toResponse(board);
    }

    @Override
    @Transactional
    public BoardResponse closeBoard(Long boardId, Long requesterId) {
        Board board = findBoard(boardId);
        requireAdmin(boardId, requesterId);

        if (board.isClosed()) {
            throw new CustomException("Board is already closed", HttpStatus.BAD_REQUEST);
        }

        board.setClosed(true);
        board.setUpdatedAt(LocalDateTime.now());
        boardRepository.save(board);

        log.info("Board closed: id={} by userId={}", boardId, requesterId);
        return toResponse(board);
    }

    @Override
    @Transactional
    public BoardResponse reopenBoard(Long boardId, Long requesterId) {
        Board board = findBoard(boardId);
        requireAdmin(boardId, requesterId);

        if (!board.isClosed()) {
            throw new CustomException("Board is already active", HttpStatus.BAD_REQUEST);
        }

        board.setClosed(false);
        board.setUpdatedAt(LocalDateTime.now());
        boardRepository.save(board);

        log.info("Board reopened: id={} by userId={}", boardId, requesterId);
        return toResponse(board);
    }

    @Override
    @Transactional
    public void deleteBoard(Long boardId, Long requesterId) {
        Board board = findBoard(boardId);

        if (!board.getCreatedById().equals(requesterId)) {
            throw new CustomException("Only the creator can delete this board",
                    HttpStatus.FORBIDDEN);
        }

        boardRepository.delete(board);
        log.info("Board deleted: id={} by userId={}", boardId, requesterId);
    }

    // ===== Member Management =====

    @Override
    @Transactional
    public BoardMember addMember(Long boardId, AddBoardMemberRequest request, Long requesterId) {
        Board board = findBoard(boardId);
        requireAdmin(boardId, requesterId);

        if (board.isClosed()) {
            throw new CustomException("Cannot add members to a closed board", HttpStatus.BAD_REQUEST);
        }

        if (memberRepository.existsByBoardIdAndUserId(boardId, request.getUserId())) {
            throw new CustomException("User already exists in this board",
                    HttpStatus.BAD_REQUEST);
        }

        BoardMember member = BoardMember.builder()
                .board(board)
                .userId(request.getUserId())
                .role(request.getRole() != null ? request.getRole() : BoardMemberRole.MEMBER)
                .addedAt(LocalDateTime.now())
                .build();

        memberRepository.save(member);

        log.info("Member added: boardId={} userId={} role={}",
                boardId, request.getUserId(), member.getRole());

        return member;
    }

    @Override
    @Transactional
    public void removeMember(Long boardId, Long userId, Long requesterId) {
        Board board = findBoard(boardId);
        requireAdmin(boardId, requesterId);

        // Prevent removing creator
        if (board.getCreatedById().equals(userId)) {
            throw new CustomException("Cannot remove the board owner", HttpStatus.BAD_REQUEST);
        }

        if (!memberRepository.existsByBoardIdAndUserId(boardId, userId)) {
            throw new CustomException("User not found in this board", HttpStatus.NOT_FOUND);
        }

        memberRepository.deleteByBoardIdAndUserId(boardId, userId);
        log.info("Member removed: boardId={} userId={}", boardId, userId);
    }

    @Override
    @Transactional
    public void updateMemberRole(Long boardId, Long userId,
                                 UpdateBoardMemberRoleRequest request,
                                 Long requesterId) {
        findBoard(boardId);
        requireAdmin(boardId, requesterId);

        BoardMember member = memberRepository
                .findByBoardIdAndUserId(boardId, userId)
                .orElseThrow(() -> new CustomException(
                        "User not found in this board", HttpStatus.NOT_FOUND));

        member.setRole(request.getRole());
        memberRepository.save(member);

        log.info("Member role updated: boardId={} userId={} role={}",
                boardId, userId, request.getRole());
    }

    @Override
    public List<BoardMember> getMembers(Long boardId) {
        findBoard(boardId);
        return memberRepository.findByBoardId(boardId);
    }

    // ===== Analytics =====

    @Override
    public BoardResponse.BoardAnalytics getBoardAnalytics(Long boardId, Long requesterId) {
        findBoard(boardId);
        requireMember(boardId, requesterId);

        List<BoardMember> members = memberRepository.findByBoardId(boardId);

        return BoardResponse.BoardAnalytics.builder()
                .totalMembers(members.size())
                .observerCount(members.stream().filter(m -> m.getRole() == BoardMemberRole.OBSERVER).count())
                .memberCount(members.stream().filter(m -> m.getRole() == BoardMemberRole.MEMBER).count())
                .adminCount(members.stream().filter(m -> m.getRole() == BoardMemberRole.ADMIN).count())
                .build();
    }

    // ===== Helpers =====

    private Board findBoard(Long boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new CustomException(
                        "Board not found", HttpStatus.NOT_FOUND));
    }

    private void requireMember(Long boardId, Long userId) {
        if (!memberRepository.existsByBoardIdAndUserId(boardId, userId)) {
            throw new CustomException(
                    "Access denied. You are not a member of this board",
                    HttpStatus.FORBIDDEN);
        }
    }

    private void requireAdmin(Long boardId, Long userId) {
        BoardMember member = memberRepository
                .findByBoardIdAndUserId(boardId, userId)
                .orElseThrow(() -> new CustomException(
                        "Access denied. You are not a member of this board",
                        HttpStatus.FORBIDDEN));

        if (member.getRole() != BoardMemberRole.ADMIN) {
            throw new CustomException(
                    "Admin access required for this action", HttpStatus.FORBIDDEN);
        }
    }

    private BoardResponse toResponse(Board board) {
        List<BoardMember> members = memberRepository.findByBoardId(board.getId());

        List<BoardResponse.MemberDTO> memberDtos = members.stream()
                .map(m -> BoardResponse.MemberDTO.builder()
                        .userId(m.getUserId())
                        .role(m.getRole())
                        .addedAt(m.getAddedAt())
                        .build())
                .toList();

        BoardResponse.BoardAnalytics analytics = BoardResponse.BoardAnalytics.builder()
                .totalMembers(members.size())
                .observerCount(members.stream().filter(m -> m.getRole() == BoardMemberRole.OBSERVER).count())
                .memberCount(members.stream().filter(m -> m.getRole() == BoardMemberRole.MEMBER).count())
                .adminCount(members.stream().filter(m -> m.getRole() == BoardMemberRole.ADMIN).count())
                .build();

        return BoardResponse.builder()
                .id(board.getId())
                .workspaceId(board.getWorkspaceId())
                .name(board.getName())
                .description(board.getDescription())
                .background(board.getBackground())
                .visibility(board.getVisibility())
                .createdById(board.getCreatedById())
                .isClosed(board.isClosed())
                .createdAt(board.getCreatedAt())
                .updatedAt(board.getUpdatedAt())
                .memberCount(members.size())
                .members(memberDtos)
                .analytics(analytics)
                .build();
    }
}