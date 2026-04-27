package com.card.dto;



import com.card.entity.CardStatus;

import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data
public class SetStatusRequest {

    @NotNull(message = "Status must be provided")
    private CardStatus status;

}