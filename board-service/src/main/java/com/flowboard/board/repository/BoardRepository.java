package com.flowboard.board.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.flowboard.board.entity.Board;
import com.flowboard.board.entity.Visibility;

import java.util.List;

public interface BoardRepository extends JpaRepository<Board, Long> {

    // Get all boards in a workspace
    List<Board> findByWorkspaceId(Long workspaceId);

    // Get all boards created by a user
    List<Board> findByCreatedById(Long createdById);

    // Get all boards where user is a member
    @Query("SELECT b FROM Board b JOIN b.members m WHERE m.userId = :userId")
    List<Board> findByMemberUserId(@Param("userId") Long userId);

    // Get boards in a workspace filtered by closed status
    List<Board> findByWorkspaceIdAndIsClosed(Long workspaceId, boolean isClosed);

    // Count boards in a workspace
    long countByWorkspaceId(Long workspaceId);

    // Get boards by visibility (e.g., PUBLIC)
    List<Board> findByVisibility(Visibility visibility);
}