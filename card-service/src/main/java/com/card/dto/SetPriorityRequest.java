package com.card.dto;


import com.card.entity.Priority;

import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data
public class SetPriorityRequest {

    @NotNull(message = "Priority must be provided")
    private Priority priority;

}