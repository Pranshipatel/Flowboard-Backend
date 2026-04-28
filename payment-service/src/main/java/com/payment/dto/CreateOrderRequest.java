package com.payment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateOrderRequest {
  @NotNull
  private Long userId;

  @NotNull
  @Min(100)
  private Long amount; // amount in paise
}