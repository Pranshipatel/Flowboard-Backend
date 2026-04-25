package com.flowboard.workspace.dto;

import com.flowboard.workspace.entity.MemberRole;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;

@Data
@Getter
public class UpdateMemberRoleRequest {

    // New role for member
    @NotNull(message = "Role is required")
    private MemberRole role;
}