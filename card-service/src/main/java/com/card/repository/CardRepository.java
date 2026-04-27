package com.card.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.card.entity.Card;
import com.card.entity.CardStatus;
import com.card.entity.Priority;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long> {

    List<Card> findByListIdAndIsArchivedFalseOrderByPosition(Long listId);

    List<Card> findByBoardIdAndIsArchivedFalse(Long boardId);

    List<Card> findByAssigneeIdAndIsArchivedFalse(Long assigneeId);

    List<Card> findByBoardIdAndStatusAndIsArchivedFalse(
            Long boardId, CardStatus status);

    List<Card> findByBoardIdAndPriorityAndIsArchivedFalse(
            Long boardId, Priority priority);

    // Overdue cards in a board
    @Query("SELECT c FROM Card c WHERE c.boardId = :boardId " +
           "AND c.isArchived = false " +
           "AND c.dueDate < :today " +
           "AND c.status != 'DONE'")
    List<Card> findOverdueByBoardId(
            @Param("boardId") Long boardId,
            @Param("today") LocalDate today);

    // All overdue cards
    @Query("SELECT c FROM Card c WHERE c.isArchived = false " +
           "AND c.dueDate < :today " +
           "AND c.status != 'DONE'")
    List<Card> findAllOverdue(@Param("today") LocalDate today);

    List<Card> findByBoardIdAndIsArchivedTrue(Long boardId);

    List<Card> findByListIdAndIsArchivedTrue(Long listId);

    long countByListIdAndIsArchivedFalse(Long listId);

    long countByBoardIdAndIsArchivedFalse(Long boardId);

    List<Card> findByDueDateAndIsArchivedFalseAndStatusNot(
            LocalDate dueDate, CardStatus status);

    @Query("SELECT MAX(c.position) FROM Card c " +
           "WHERE c.listId = :listId AND c.isArchived = false")
    Optional<Integer> findMaxPositionByListId(
            @Param("listId") Long listId);

    @Modifying
    @Query("UPDATE Card c SET c.position = c.position + 1 " +
           "WHERE c.listId = :listId " +
           "AND c.position >= :fromPosition " +
           "AND c.isArchived = false")
    void shiftPositionsRight(
            @Param("listId") Long listId,
            @Param("fromPosition") int fromPosition);

    @Modifying
    @Query("UPDATE Card c SET c.position = c.position - 1 " +
           "WHERE c.listId = :listId " +
           "AND c.position > :fromPosition " +
           "AND c.isArchived = false")
    void shiftPositionsLeft(
            @Param("listId") Long listId,
            @Param("fromPosition") int fromPosition);

    @Query("SELECT c FROM Card c WHERE c.boardId = :boardId " +
           "AND c.isArchived = false " +
           "AND LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Card> searchByTitle(
            @Param("boardId") Long boardId,
            @Param("keyword") String keyword);

    @Query("SELECT c FROM Card c WHERE c.isArchived = false " +
           "AND (LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR c.assigneeId = :assigneeId)")
    List<Card> searchByTitleOrAssignee(
            @Param("keyword") String keyword,
            @Param("assigneeId") Long assigneeId);

}