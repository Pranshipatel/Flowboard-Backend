package com.flowboard.workspace.dto;


import com.flowboard.workspace.entity.Visibility;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateWorkspaceRequest {

    // Workspace name
    @NotBlank(message = "Workspace name is required")
    @Size(min = 2, max = 50, message = "Name must be 2-50 characters")
    private String name;

    private String description;

    private Visibility visibility;

    private String logoUrl;
}