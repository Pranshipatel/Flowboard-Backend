package com.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VerifyPaymentRequest {
  @NotNull
  private Long userId;

  @NotBlank
  private String razorpayOrderId;

  @NotBlank
  private String razorpayPaymentId;

  @NotBlank
  private String razorpaySignature;
}