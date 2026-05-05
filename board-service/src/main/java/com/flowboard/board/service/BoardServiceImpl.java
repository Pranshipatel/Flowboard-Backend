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
import com.flowboard.board.client.WorkspaceClient;
import com.flowboard.board.client.CardClient;
import com.flowboard.board.client.CardClient.BoardStatsResponse;
import com.flowboard.board.dto.WorkspaceResponse;
import com.flowboard.board.dto.WorkspaceMemberResponse;
import com.flowboard.board.dto.SendNotificationRequest;
import com.flowboard.board.dto.PublicBoardDetailResponse;
import com.flowboard.board.dto.PublicCardClientResponse;
import com.flowboard.board.dto.PublicListClientResponse;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import com.flowboard.board.config.RabbitMQConfig;

import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import com.flowboard.board.client.ListClient;


@Service
@RequiredArgsConstructor
@Slf4j
public class BoardServiceImpl implements BoardService {

    private static final long FREE_BOARD_LIMIT = 2;

    private final com.flowboard.board.repository.BoardRepository boardRepository;
    private final BoardMemberRepository memberRepository;
    private final WorkspaceClient workspaceClient;
    private final CardClient cardClient;
    private final ListClient listClient;
    private final RabbitTemplate rabbitTemplate;

    // ===== Board CRUD =====

    @Override
    @Transactional
    public BoardResponse createBoard(CreateBoardRequest request, Long createdById, boolean premium) {

        WorkspaceResponse workspace = workspaceClient.getWorkspaceById(request.getWorkspaceId(), createdById);
        if (!workspace.getOwnerId().equals(createdById)) {
            throw new CustomException("Only the owner of the workspace can create a board", HttpStatus.FORBIDDEN);
        }

        if (!premium && boardRepository.countByWorkspaceId(request.getWorkspaceId()) >= FREE_BOARD_LIMIT) {
            throw new CustomException(
                    "Free users can create up to 2 boards per workspace. Upgrade to premium for unlimited boards.",
                    HttpStatus.FORBIDDEN
            );
        }

        Visibility boardVisibility = isPublicWorkspace(workspace)
                ? (request.getVisibility() != null ? request.getVisibility() : Visibility.PRIVATE)
                : Visibility.PRIVATE;

        Board board = Board.builder()
                .workspaceId(request.getWorkspaceId())
                .name(request.getName())
                .description(request.getDescription())
                .background(request.getBackground())
                .visibility(boardVisibility)
                .createdById(createdById)
                .isClosed(false)
                .dueDate(request.getDueDate())
                .priority(request.getPriority())
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

        // Create Default Lists
        try {
            String[] defaultLists = premium
                    ? new String[] {"To Do", "In Progress", "In Review", "Done"}
                    : new String[] {"To Do", "In Progress"};
            for (int i = 0; i < defaultLists.length; i++) {
                listClient.createList(
                        java.util.Map.of(
                                "boardId", board.getId(),
                                "name", defaultLists[i],
                                "position", i
                        ),
                        createdById,
                        premium ? "PREMIUM" : "FREE",
                        premium ? "ACTIVE" : "EXPIRED"
                );
            }
        } catch (Exception e) {
            log.error("Failed to create default lists for board id={}", board.getId(), e);
        }

        // Notify workspace members
        try {
            List<WorkspaceMemberResponse> members = workspaceClient.getMembers(request.getWorkspaceId(), createdById);
            for (WorkspaceMemberResponse member : members) {
                if (!member.getUserId().equals(createdById)) {
                    SendNotificationRequest notification = SendNotificationRequest.builder()
                            .recipientId(member.getUserId())
                            .actorId(createdById)
                            .type("ASSIGNMENT")
                            .title("New Task Created")
                            .message("The task '" + board.getName() + "' has been created in workspace '" + workspace.getName() + "'.")
                            .relatedId(board.getId())
                            .relatedType("BOARD")
                            .deepLinkUrl("/b/" + board.getId())
                            .sendEmail(false)
                            .build();

                    rabbitTemplate.convertAndSend(
                            RabbitMQConfig.NOTIFICATION_EXCHANGE,
                            RabbitMQConfig.NOTIFICATION_ROUTING_KEY,
                            notification
                    );
                }
            }
        } catch (Exception e) {
            log.error("Failed to send board creation notifications", e);
        }

        return toResponse(board);
    }

    @Override
    public BoardResponse getBoardById(Long boardId, Long requesterId) {
        Board board = findBoard(boardId);
        WorkspaceResponse workspace = workspaceClient.getWorkspaceById(board.getWorkspaceId(), requesterId);
        if (board.getVisibility() == Visibility.PRIVATE && !isWorkspaceParticipant(workspace, requesterId)) {
            throw new CustomException("Access denied. This board is private", HttpStatus.FORBIDDEN);
        }
        return toResponse(board);
    }

    @Override
    public List<BoardResponse> getBoardsByWorkspace(Long workspaceId, Long requesterId, boolean premium) {
        WorkspaceResponse workspace = workspaceClient.getWorkspaceById(workspaceId, requesterId);
        boolean participant = isWorkspaceParticipant(workspace, requesterId);

        List<Board> boards = boardRepository.findByWorkspaceId(workspaceId);
        if (!participant) {
            boards = boards.stream()
                    .filter(board -> board.getVisibility() == Visibility.PUBLIC)
                    .toList();
        }

        if (!premium) {
            boards = boards.stream().limit(FREE_BOARD_LIMIT).toList();
        }
        return toResponses(boards);
    }

    @Override
    public List<BoardResponse> getBoardsByMember(Long userId) {
        return toResponses(boardRepository.findByMemberUserId(userId));
    }

    @Override
    public List<BoardResponse> getBoardsByCreator(Long createdById) {
        return toResponses(boardRepository.findByCreatedById(createdById));
    }

    @Override
    public List<BoardResponse> getPublicBoards() {
        return toResponses(boardRepository.findByVisibility(Visibility.PUBLIC));
    }

    @Override
    public PublicBoardDetailResponse getPublicBoardDetail(Long boardId) {
        Board board = findBoard(boardId);
        if (board.getVisibility() != Visibility.PUBLIC || board.isClosed()) {
            throw new CustomException("Board is not publicly available", HttpStatus.FORBIDDEN);
        }

        WorkspaceResponse workspace = workspaceClient.getPublicWorkspaceById(board.getWorkspaceId());
        if (!isPublicWorkspace(workspace)) {
            throw new CustomException("Workspace is private", HttpStatus.FORBIDDEN);
        }

        return toPublicDetailResponse(board);
    }

    @Override
    public List<PublicBoardDetailResponse> getPublicBoardDetailsByWorkspace(Long workspaceId) {
        WorkspaceResponse workspace = workspaceClient.getPublicWorkspaceById(workspaceId);
        if (workspace.getVisibility() == null || !"PUBLIC".equalsIgnoreCase(workspace.getVisibility().toString())) {
            throw new CustomException("Workspace is private", HttpStatus.FORBIDDEN);
        }

        return boardRepository.findByWorkspaceId(workspaceId)
                .stream()
                .filter(board -> board.getVisibility() == Visibility.PUBLIC && !board.isClosed())
                .map(this::toPublicDetailResponse)
                .toList();
    }

    @Override
    public List<BoardResponse> getClosedBoards(Long workspaceId, Long requesterId) {
        List<Board> boards = boardRepository.findByWorkspaceIdAndIsClosed(workspaceId, true)
                .stream()
                .filter(b -> memberRepository.existsByBoardIdAndUserId(b.getId(), requesterId))
                .toList();
        return toResponses(boards);
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
        if (request.getVisibility() != null) {
            WorkspaceResponse workspace = workspaceClient.getWorkspaceById(board.getWorkspaceId(), requesterId);
            board.setVisibility(isPublicWorkspace(workspace) ? request.getVisibility() : Visibility.PRIVATE);
        }
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

        boolean isCreator = board.getCreatedById().equals(requesterId);
        boolean isAdmin = false;
        
        if (!isCreator) {
            BoardMember member = memberRepository.findByBoardIdAndUserId(boardId, requesterId).orElse(null);
            isAdmin = member != null && member.getRole() == BoardMemberRole.ADMIN;
        }

        if (!isCreator && !isAdmin) {
            throw new CustomException("Only the creator or an admin can delete this board",
                    HttpStatus.FORBIDDEN);
        }

        boardRepository.delete(board);
        log.info("Board deleted: id={} by userId={}", boardId, requesterId);

        // Notify workspace members
        try {
            WorkspaceResponse workspace = workspaceClient.getWorkspaceById(board.getWorkspaceId(), requesterId);
            List<WorkspaceMemberResponse> members = workspaceClient.getMembers(board.getWorkspaceId(), requesterId);
            for (WorkspaceMemberResponse member : members) {
                if (!member.getUserId().equals(requesterId)) {
                    SendNotificationRequest notification = SendNotificationRequest.builder()
                            .recipientId(member.getUserId())
                            .actorId(requesterId)
                            .type("BROADCAST")
                            .title("Task Deleted")
                            .message("The task '" + board.getName() + "' has been deleted from workspace '" + workspace.getName() + "'.")
                            .relatedId(board.getWorkspaceId())
                            .relatedType("WORKSPACE")
                            .build();

                    rabbitTemplate.convertAndSend(
                            RabbitMQConfig.NOTIFICATION_EXCHANGE,
                            RabbitMQConfig.NOTIFICATION_ROUTING_KEY,
                            notification
                    );
                }
            }
        } catch (Exception e) {
            log.error("Failed to send board deletion notifications", e);
        }
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

    private boolean isPublicWorkspace(WorkspaceResponse workspace) {
        return workspace.getVisibility() != null && "PUBLIC".equalsIgnoreCase(workspace.getVisibility());
    }

    private boolean isWorkspaceParticipant(WorkspaceResponse workspace, Long userId) {
        if (workspace.getOwnerId() != null && workspace.getOwnerId().equals(userId)) {
            return true;
        }

        try {
            return workspaceClient.getMembers(workspace.getId(), userId)
                    .stream()
                    .anyMatch(member -> member.getUserId().equals(userId));
        } catch (Exception e) {
            return false;
        }
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

    private List<BoardResponse> toResponses(List<Board> boards) {
        if (boards.isEmpty()) return List.of();
        List<Long> boardIds = boards.stream().map(Board::getId).toList();
        
        Map<Long, BoardStatsResponse> tempMap;
        try {
            List<BoardStatsResponse> stats = cardClient.getBoardStats(boardIds);
            tempMap = stats.stream().collect(Collectors.toMap(BoardStatsResponse::boardId, s -> s));
        } catch (Exception e) {
            log.error("Failed to fetch board stats from card-service", e);
            tempMap = Map.of();
        }

        final Map<Long, BoardStatsResponse> finalStatsMap = tempMap;

        return boards.stream().map(board -> {
            BoardStatsResponse stat = finalStatsMap.get(board.getId());
            long total = stat != null ? stat.totalCards() : 0;
            long done = stat != null ? stat.doneCards() : 0;
            return toResponse(board, total, done);
        }).toList();
    }

    private BoardResponse toResponse(Board board) {
        return toResponse(board, 0, 0);
    }

    private BoardResponse toResponse(Board board, long totalCards, long doneCards) {
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

        int progressPercentage = totalCards > 0 ? (int) ((doneCards * 100) / totalCards) : 0;

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
                .dueDate(board.getDueDate())
                .priority(board.getPriority())
                .memberCount(members.size())
                .members(memberDtos)
                .analytics(analytics)
                .totalCards(totalCards)
                .doneCards(doneCards)
                .progressPercentage(progressPercentage)
                .build();
    }

    private PublicBoardDetailResponse toPublicDetailResponse(Board board) {
        List<PublicCardClientResponse> cards = cardClient.getByBoard(board.getId(), "PREMIUM", "ACTIVE");
        List<PublicListClientResponse> lists = listClient.getByBoard(board.getId(), "PREMIUM", "ACTIVE");

        return PublicBoardDetailResponse.builder()
                .id(board.getId())
                .workspaceId(board.getWorkspaceId())
                .name(board.getName())
                .description(board.getDescription())
                .background(board.getBackground())
                .visibility(board.getVisibility())
                .isClosed(board.isClosed())
                .createdAt(board.getCreatedAt())
                .updatedAt(board.getUpdatedAt())
                .dueDate(board.getDueDate())
                .priority(board.getPriority())
                .lists(lists.stream()
                        .map(list -> PublicBoardDetailResponse.PublicListDto.builder()
                                .id(list.getId())
                                .boardId(list.getBoardId())
                                .name(list.getName())
                                .position(list.getPosition())
                                .color(list.getColor())
                                .cards(cards.stream()
                                        .filter(card -> list.getId().equals(card.getListId()))
                                        .map(card -> PublicBoardDetailResponse.PublicCardDto.builder()
                                                .id(card.getId())
                                                .listId(card.getListId())
                                                .boardId(card.getBoardId())
                                                .title(card.getTitle())
                                                .description(card.getDescription())
                                                .position(card.getPosition())
                                                .priority(card.getPriority())
                                                .status(card.getStatus())
                                                .startDate(card.getStartDate())
                                                .dueDate(card.getDueDate())
                                                .isOverdue(card.isOverdue())
                                                .coverColor(card.getCoverColor())
                                                .build())
                                        .toList())
                                .build())
                        .toList())
                .build();
    }
}
