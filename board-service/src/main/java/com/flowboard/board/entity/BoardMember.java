package com.flowboard.board.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "board_members",
    uniqueConstraints = @UniqueConstraint(columnNames = {"board_id", "user_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Associated board
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    /**
     * User ID of the member
     */
    @Column(nullable = false)
    private Long userId;

    /**
     * Role of the member inside board
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BoardMemberRole role = BoardMemberRole.MEMBER;

    /**
     * Timestamp when member was added
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime addedAt;

    /* ================= Lifecycle ================= */

    @PrePersist
    public void onAdd() {
        this.addedAt = LocalDateTime.now();
    }

    /* ================= Domain Logic ================= */

    public void promoteToAdmin() {
        this.role = BoardMemberRole.ADMIN;
    }

    public void demoteToMember() {
        this.role = BoardMemberRole.MEMBER;
    }
}