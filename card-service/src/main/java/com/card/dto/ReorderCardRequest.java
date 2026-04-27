package com.card.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

import java.util.List;

@Data
public class ReorderCardRequest {

    @NotNull(message = "List ID is required")
    private Long listId;

    @NotEmpty(message = "Order of card is required")
    private List<Long> orderedCardIds;

}