package com.flowboard.list.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class MoveListRequest {

    // Target Board
    @NotNull(message = "Target board ID is required")
    private Long targetBoardId;

    // Position within target board
    @PositiveOrZero(message = "Position must be 0 or greater")
    private Integer targetPosition;
}