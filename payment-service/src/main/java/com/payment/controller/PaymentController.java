package com.payment.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.payment.dto.CreateOrderRequest;
import com.payment.dto.CreateOrderResponse;
import com.payment.dto.VerifyPaymentRequest;
import com.payment.service.PaymentSubscriptionService;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

  private final PaymentSubscriptionService paymentService;

  @PostMapping("/create-order")
  public ResponseEntity<CreateOrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
    return ResponseEntity.ok(paymentService.createOrder(request.getUserId(), request.getAmount()));
  }

  @PostMapping("/verify")
  public ResponseEntity<Void> verify(@Valid @RequestBody VerifyPaymentRequest request) {
    paymentService.verifyAndActivatePremium(request);
    return ResponseEntity.ok().build();
  }
}