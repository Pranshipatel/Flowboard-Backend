package com.flowboard.board.dto;


import com.flowboard.board.entity.BoardPriority;
import com.flowboard.board.entity.Visibility;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateBoardRequest {

    /**
     * Workspace to which this board belongs
     */
    @NotNull(message = "Workspace ID is required")
    private Long workspaceId;

    /**
     * Name of the board (2–100 characters)
     */
    @NotBlank(message = "Board name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    /**
     * Optional description
     */
    private String description;

    /**
     * Background (color/image identifier)
     */
    private String background;

    /**
     * Visibility of the board (default: PRIVATE)
     */
    private Visibility visibility = Visibility.PRIVATE;

    /**
     * Due date for the board
     */
    private java.time.LocalDateTime dueDate;

    /**
     * Priority of the board
     */
    private BoardPriority priority;

    /* ================= Utility ================= */

    public Visibility resolveVisibility() {
        return visibility != null ? visibility : Visibility.PRIVATE;
    }
}