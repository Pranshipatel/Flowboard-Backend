package com.flowboard.workspace.dto;


import com.flowboard.workspace.entity.Visibility;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateWorkspaceRequest {

    // Workspace name
    @NotBlank(message = "Workspace name is required")
    @Size(min = 2, max = 50, message = "Name must be 2-50 charcters long")
    private String name;

    private String description;

    // Default visibility
    private Visibility visibility = Visibility.PRIVATE;

    private String logoUrl;
}