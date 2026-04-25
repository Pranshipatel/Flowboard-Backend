package com.flowboard.board.dto;


import com.flowboard.board.entity.BoardMemberRole;

import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotNull;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AddBoardMemberRequest {

    /**
     * ID of the user to be added to the board
     */
    @NotNull(message = "User ID is required")
    private Long userId;

    /**
     * Role assigned to the user 
     */
    private BoardMemberRole role = BoardMemberRole.MEMBER;

    /* ================= Utility ================= */

    public BoardMemberRole resolveRole() {
        return role != null ? role : BoardMemberRole.MEMBER;
    }
}