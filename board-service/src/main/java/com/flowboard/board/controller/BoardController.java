package com.flowboard.board.controller;

import com.flowboard.board.dto.AddBoardMemberRequest;
import com.flowboard.board.dto.BoardResponse;
import com.flowboard.board.dto.CreateBoardRequest;
import com.flowboard.board.dto.PublicBoardDetailResponse;
import com.flowboard.board.dto.UpdateBoardMemberRoleRequest;
import com.flowboard.board.dto.UpdateBoardRequest;
import com.flowboard.board.entity.BoardMember;
import com.flowboard.board.exception.CustomException;
import com.flowboard.board.service.BoardService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/boards")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    private Long resolveUserId(Long userIdHeader) {
        if (userIdHeader != null) {
            return userIdHeader;
        }

        throw new CustomException("User identification header is missing", HttpStatus.BAD_REQUEST);
    }

    private boolean isPremium(String plan, String status) {
        return "PREMIUM".equalsIgnoreCase(plan) && "ACTIVE".equalsIgnoreCase(status);
    }

    @PostMapping
    public ResponseEntity<BoardResponse> create(
            @Valid @RequestBody CreateBoardRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader,
            @RequestHeader(value = "X-Subscription-Plan", required = false, defaultValue = "FREE") String plan,
            @RequestHeader(value = "X-Subscription-Status", required = false, defaultValue = "EXPIRED") String status
    ) {
        Long userId = resolveUserId(userIdHeader);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(boardService.createBoard(request, userId, isPremium(plan, status)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BoardResponse> getById(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader
    ) {
        Long userId = resolveUserId(userIdHeader);
        return ResponseEntity.ok(boardService.getBoardById(id, userId));
    }

    @GetMapping("/workspace/{workspaceId}")
    public ResponseEntity<List<BoardResponse>> getByWorkspace(
            @PathVariable Long workspaceId,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader,
            @RequestHeader(value = "X-Subscription-Plan", required = false, defaultValue = "FREE") String plan,
            @RequestHeader(value = "X-Subscription-Status", required = false, defaultValue = "EXPIRED") String status
    ) {
        Long userId = resolveUserId(userIdHeader);
        return ResponseEntity.ok(boardService.getBoardsByWorkspace(workspaceId, userId, isPremium(plan, status)));
    }

    @GetMapping("member/{userId}")
    public ResponseEntity<List<BoardResponse>> getByMember(@PathVariable Long userId){
        return ResponseEntity.ok(boardService.getBoardsByMember(userId));
    }

    @GetMapping("/creator/{createdById}")
    public ResponseEntity<List<BoardResponse>> getByCreator(@PathVariable Long createdById){
        return ResponseEntity.ok(boardService.getBoardsByCreator(createdById));
    }

    @GetMapping("/public")
    public ResponseEntity<List<BoardResponse>> getPublic(){
        return ResponseEntity.ok(boardService.getPublicBoards());
    }

    @GetMapping("/public/{id}/details")
    public ResponseEntity<PublicBoardDetailResponse> getPublicBoardDetail(@PathVariable Long id){
        return ResponseEntity.ok(boardService.getPublicBoardDetail(id));
    }

    @GetMapping("/public/workspace/{workspaceId}/details")
    public ResponseEntity<List<PublicBoardDetailResponse>> getPublicWorkspaceBoardDetails(@PathVariable Long workspaceId){
        return ResponseEntity.ok(boardService.getPublicBoardDetailsByWorkspace(workspaceId));
    }

    @GetMapping("/workspace/{workspaceId}/closed")
    public ResponseEntity<List<BoardResponse>> getClosedBoards(
            @PathVariable Long workspaceId,
            @RequestHeader(name = "X-User-Id", required = false) Long userIdHeader
    ) {
        Long userId = resolveUserId(userIdHeader);
        return ResponseEntity.ok(boardService.getClosedBoards(workspaceId, userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BoardResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBoardRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader
    ) {
        Long userId = resolveUserId(userIdHeader);
        return ResponseEntity.ok(boardService.updateBoard(id, request, userId));
    }

    @PutMapping("/{id}/close")
    public ResponseEntity<BoardResponse> close(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader
    ) {
        Long userId = resolveUserId(userIdHeader);
        return ResponseEntity.ok(boardService.closeBoard(id, userId));
    }

    @PutMapping("/{id}/reopen")
    public ResponseEntity<BoardResponse> reopen(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader
    ) {
        Long userId = resolveUserId(userIdHeader);
        return ResponseEntity.ok(boardService.reopenBoard(id, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader
    ) {
        Long userId = resolveUserId(userIdHeader);
        boardService.deleteBoard(id, userId);
        return ResponseEntity.ok("Board removed successfully");
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<BoardMember> addMember(
            @PathVariable Long id,
            @Valid @RequestBody AddBoardMemberRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader
    ) {
        Long userId = resolveUserId(userIdHeader);
        return ResponseEntity.ok(boardService.addMember(id, request, userId));
    }

    @DeleteMapping("/{id}/members/{memberId}")
    public ResponseEntity<String> deleteMember(
            @PathVariable Long id,
            @PathVariable Long memberId,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader
    ) {
        Long userId = resolveUserId(userIdHeader);
        boardService.removeMember(id, memberId, userId);
        return ResponseEntity.ok("Member removed successfully");
    }

    @PutMapping("/{id}/members/{memberId}/role")
    public ResponseEntity<String> updateMemberRole(
            @PathVariable Long id,
            @PathVariable Long memberId,
            @Valid @RequestBody UpdateBoardMemberRoleRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader
    ) {
        Long userId = resolveUserId(userIdHeader);
        boardService.updateMemberRole(id, memberId, request, userId);
        return ResponseEntity.ok("Member role updated");
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<BoardMember>> getMembers(@PathVariable Long id){
        return ResponseEntity.ok(boardService.getMembers(id));
    }

    @GetMapping("/{id}/analytics")
    public ResponseEntity<BoardResponse.BoardAnalytics> getAnalytics(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader
    ) {
        Long userId = resolveUserId(userIdHeader);
        return ResponseEntity.ok(boardService.getBoardAnalytics(id, userId));
    }
}
