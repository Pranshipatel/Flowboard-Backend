package com.flowboard.workspace.dto;


import com.flowboard.workspace.entity.MemberRole;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddMemberRequest {

    // User to be added
    @NotNull(message = "User ID is required")
    private Long userId;

    // Default role
    private MemberRole role = MemberRole.MEMBER;
}