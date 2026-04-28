package com.payment.dto;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateOrderResponse {
  private String razorpayOrderId;
  private Long   amount;     // in paise (100 paise = 1 INR)
  private String currency;
  private String keyId;      // Razorpay publishable key sent to frontend
}