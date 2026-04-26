package com.flowboard.list.dto;


import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ReorderListRequest {

    // Board reference
    @NotNull(message = "Board ID is required")
    private Long boardId;

    // Ordered list of IDs
    @NotEmpty(message = "List order is required")
    private List<@NotNull(message = "List ID cannot be null") Long> orderedListIds;
}