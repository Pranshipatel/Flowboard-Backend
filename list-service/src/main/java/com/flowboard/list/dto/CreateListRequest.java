package com.flowboard.list.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateListRequest {

    @NotNull(message = "Board ID is required")
    private Long boardId;

    @NotBlank(message = "List name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @PositiveOrZero(message = "Position must be 0 or greater")
    private Integer position;

    @Size(max = 20, message = "Color value is too long")
    private String color;
}