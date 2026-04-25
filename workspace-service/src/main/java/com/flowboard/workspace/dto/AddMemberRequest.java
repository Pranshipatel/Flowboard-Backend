package com.flowboard.workspace.dto;


import com.flowboard.workspace.entity.MemberRole;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddMemberRequest {

    // User to be added
    @NotBlank(message = "User ID is required")
    private Long userId;

    // Default role
    private MemberRole role = MemberRole.MEMBER;
}