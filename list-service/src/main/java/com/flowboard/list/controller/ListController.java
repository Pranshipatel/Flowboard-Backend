package com.flowboard.list.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.flowboard.list.dto.CreateListRequest;
import com.flowboard.list.dto.ListResponse;
import com.flowboard.list.dto.MoveListRequest;
import com.flowboard.list.dto.ReorderListRequest;
import com.flowboard.list.dto.UpdateListRequest;
import com.flowboard.list.exception.CustomException;
import com.flowboard.list.service.ListService;

import java.util.List;

@RestController
@RequestMapping("api/v1/lists")
@RequiredArgsConstructor
public class ListController {

    private final ListService listService;

    // ================= HELPER =================

    private Long resolveUserId(Long userIdHeader) {
        if (userIdHeader != null) return userIdHeader;
        throw new CustomException("Missing required header: X-User-Id", HttpStatus.BAD_REQUEST);
    }

    // ================= CREATE =================

    @PostMapping
    public ResponseEntity<ListResponse> create(
            @Valid @RequestBody CreateListRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(listService.createList(request, resolveUserId(userId)));
    }

    // ================= READ =================

    @GetMapping("/{id}")
    public ResponseEntity<ListResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(listService.getListById(id));
    }

    @GetMapping("/board/{boardId}")
    public ResponseEntity<List<ListResponse>> getByBoard(@PathVariable Long boardId) {
        return ResponseEntity.ok(listService.getListsByBoard(boardId));
    }

    @GetMapping("/board/{boardId}/archived")
    public ResponseEntity<List<ListResponse>> getArchived(@PathVariable Long boardId) {
        return ResponseEntity.ok(listService.getArchivedLists(boardId));
    }

    // ================= UPDATE =================

    @PutMapping("/{id}")
    public ResponseEntity<ListResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateListRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        return ResponseEntity.ok(
                listService.updateList(id, request, resolveUserId(userId))
        );
    }

    @PutMapping("/reorder")
    public ResponseEntity<List<ListResponse>> reorder(
            @Valid @RequestBody ReorderListRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        return ResponseEntity.ok(
                listService.reorderLists(request, resolveUserId(userId))
        );
    }

    @PutMapping("/{id}/move")
    public ResponseEntity<ListResponse> move(
            @PathVariable Long id,
            @Valid @RequestBody MoveListRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        return ResponseEntity.ok(
                listService.moveList(id, request, resolveUserId(userId))
        );
    }

    // ================= DELETE =================

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        listService.deleteList(id, resolveUserId(userId));
        return ResponseEntity.ok("List deleted successfully.");
    }

    // ================= ARCHIVE =================

    @PutMapping("/{id}/archive")
    public ResponseEntity<ListResponse> archive(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        return ResponseEntity.ok(
                listService.archiveList(id, resolveUserId(userId))
        );
    }

    @PutMapping("/{id}/unarchive")
    public ResponseEntity<ListResponse> unarchive(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        return ResponseEntity.ok(
                listService.unarchiveList(id, resolveUserId(userId))
        );
    }
}