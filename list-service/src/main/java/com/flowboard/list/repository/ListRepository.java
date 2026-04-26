package com.flowboard.list.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.flowboard.list.entity.TaskList;

import java.util.List;
import java.util.Optional;

public interface ListRepository extends JpaRepository<TaskList, Long> {

    // ================= BASIC FETCH =================

    List<TaskList> findByBoardId(Long boardId);

    Optional<TaskList> findByIdAndBoardId(Long id, Long boardId);


    // ================= ARCHIVE FILTER =================

    List<TaskList> findByBoardIdAndIsArchivedFalseOrderByPosition(Long boardId);

    List<TaskList> findByBoardIdAndIsArchivedTrue(Long boardId);

    long countByBoardIdAndIsArchivedFalse(Long boardId);


    // ================= POSITION =================

    @Query("""
        SELECT MAX(t.position)
        FROM TaskList t
        WHERE t.boardId = :boardId
          AND t.isArchived = false
    """)
    Optional<Integer> findMaxPositionByBoardId(@Param("boardId") Long boardId);

    boolean existsByBoardIdAndPositionAndIsArchivedFalse(Long boardId, int position);


    // ================= SHIFT OPERATIONS =================

    @Modifying
    @Query("""
        UPDATE TaskList t
        SET t.position = t.position + 1
        WHERE t.boardId = :boardId
          AND t.position >= :fromPosition
          AND t.isArchived = false
    """)
    void shiftPositionsRight(@Param("boardId") Long boardId,
                             @Param("fromPosition") int fromPosition);

    @Modifying
    @Query("""
        UPDATE TaskList t
        SET t.position = t.position - 1
        WHERE t.boardId = :boardId
          AND t.position > :fromPosition
          AND t.isArchived = false
    """)
    void shiftPositionsLeft(@Param("boardId") Long boardId,
                            @Param("fromPosition") int fromPosition);
}