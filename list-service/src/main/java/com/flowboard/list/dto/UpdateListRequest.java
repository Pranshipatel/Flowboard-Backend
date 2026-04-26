package com.flowboard.list.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateListRequest {

    // List name
    @NotBlank(message = "List name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    // Optional color
    @Size(max = 20, message = "Color value is too long")
    private String color;
}