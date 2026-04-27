package com.card.dto;



import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

import java.time.LocalDate;

import com.card.entity.CardStatus;
import com.card.entity.Priority;

@Data
public class UpdateCardRequest {

    @NotBlank(message = "Title must be provided")
    @Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
    private String title;

    private String description;

    private Priority priority;
    private CardStatus status;

    private LocalDate startDate;
    private LocalDate dueDate;

    private String coverColor;

}