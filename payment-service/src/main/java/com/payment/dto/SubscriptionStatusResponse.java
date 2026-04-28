package com.payment.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubscriptionStatusResponse {
  private Long userId;
  private String plan;   // FREE|PREMIUM
  private String status; // ACTIVE|EXPIRED
}