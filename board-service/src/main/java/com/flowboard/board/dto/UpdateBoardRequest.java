package com.flowboard.board.dto;

import com.flowboard.board.entity.Visibility;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateBoardRequest {

    // Board name must be provided and should be between 2 and 100 characters
    @NotBlank(message = "Board name is required")
    @Size(min = 2, max = 100, message = "Name must be 2-100 characters")
    private String name;

    // Optional description update
    private String description;

    // Optional background (color/image)
    private String background;

    // Optional visibility update (PRIVATE / PUBLIC etc.)
    private Visibility visibility;
}