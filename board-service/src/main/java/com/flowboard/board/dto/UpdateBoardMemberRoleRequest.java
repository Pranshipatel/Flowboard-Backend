package com.flowboard.board.dto;

import com.flowboard.board.entity.BoardMemberRole;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateBoardMemberRoleRequest {

    /**
     * New role to assign to the member
     */
    @NotNull(message = "Role is required")
    private BoardMemberRole role;
    
}
