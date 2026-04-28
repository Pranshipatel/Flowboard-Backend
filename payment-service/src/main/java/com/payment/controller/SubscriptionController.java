package com.payment.controller;

import com.payment.dto.SubscriptionStatusResponse;
import com.payment.service.PaymentSubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/subscription")
@RequiredArgsConstructor
public class SubscriptionController {

  private final PaymentSubscriptionService paymentService;

  @GetMapping("/status/{userId}")
  public ResponseEntity<SubscriptionStatusResponse> status(@PathVariable Long userId) {
    return ResponseEntity.ok(paymentService.getSubscriptionStatus(userId));
  }
}