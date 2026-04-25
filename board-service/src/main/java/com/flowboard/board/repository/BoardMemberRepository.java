package com.flowboard.board.repository;


import org.springframework.data.jpa.repository.JpaRepository;

import com.flowboard.board.entity.BoardMember;
import com.flowboard.board.entity.BoardMemberRole;

import java.util.List;
import java.util.Optional;

public interface BoardMemberRepository extends JpaRepository<BoardMember, Long> {

    // Members of a board
    List<BoardMember> findByBoardId(Long boardId);

    // Specific member in a board
    Optional<BoardMember> findByBoardIdAndUserId(Long boardId, Long userId);

    // Check if user is already a member
    boolean existsByBoardIdAndUserId(Long boardId, Long userId);

    // Remove member from board
    void deleteByBoardIdAndUserId(Long boardId, Long userId);

    // Members by role (ADMIN / MEMBER / OBSERVER)
    List<BoardMember> findByBoardIdAndRole(Long boardId, BoardMemberRole role);

    // All boards a user is part of
    List<BoardMember> findByUserId(Long userId);
}