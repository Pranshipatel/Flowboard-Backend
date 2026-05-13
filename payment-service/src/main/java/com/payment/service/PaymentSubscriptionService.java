package com.payment.service;

import com.payment.dto.CreateOrderResponse;
import com.payment.dto.SubscriptionStatusResponse;
import com.payment.dto.VerifyPaymentRequest;
import com.payment.entity.PlanType;
import com.payment.entity.Subscription;
import com.payment.entity.SubscriptionStatus;
import com.payment.exception.PaymentProcessingException;
import com.payment.razorpay.RazorpaySignatureVerifier;
import com.payment.repository.SubscriptionRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentSubscriptionService {

  private final RazorpayClient              razorpayClient;
  private final RazorpaySignatureVerifier   signatureVerifier;
  private final SubscriptionRepository      subscriptionRepository;

  @Value("${razorpay.keyId}")
  private String razorpayKeyId;

  /** Skip Razorpay signature check in dev/test mode (default: false). */
  @Value("${razorpay.skipVerification:false}")
  private boolean skipVerification;

  // ─── CREATE ORDER ──────────────────────────────────────────────

  public CreateOrderResponse createOrder(Long userId, Long amountInr) {
    try {
      long amountPaise = amountInr * 100L;  // Razorpay expects paise (100 paise = 1 INR)

      JSONObject options = new JSONObject();
      options.put("amount",          amountPaise);
      options.put("currency",        "INR");
      options.put("receipt",         "user_" + userId + "_ts_" + System.currentTimeMillis());
      options.put("payment_capture", 1);

      Order order = razorpayClient.orders.create(options);
      long returnedAmount = ((Number) order.get("amount")).longValue(); // in paise

      log.info("Razorpay order created: userId={} orderId={} amount={}p", userId, order.get("id"), returnedAmount);

      return CreateOrderResponse.builder()
          .razorpayOrderId(order.get("id").toString())
          .amount(returnedAmount)   // paise — frontend should use this directly
          .currency(order.get("currency").toString())
          .keyId(razorpayKeyId)     // send publishable key to frontend
          .build();

    } catch (Exception e) {
      log.error("Razorpay createOrder failed: userId={} amountInr={} err={}", userId, amountInr, e.getMessage(), e);
      throw new PaymentProcessingException("Failed to create Razorpay order: " + e.getMessage(), e);
    }
  }

  // ─── VERIFY & ACTIVATE ─────────────────────────────────────────

  @Transactional
  public void verifyAndActivatePremium(VerifyPaymentRequest req) {

    // Idempotency: if paymentId was already processed, skip
    if (subscriptionRepository.findByRazorpayPaymentId(req.getRazorpayPaymentId()).isPresent()) {
      log.info("Payment already processed: paymentId={}", req.getRazorpayPaymentId());
      return;
    }

    // Signature verification (skippable in dev/test mode)
    if (!skipVerification) {
      boolean ok = signatureVerifier.verify(
          req.getRazorpayOrderId(),
          req.getRazorpayPaymentId(),
          req.getRazorpaySignature()
      );
      if (!ok) {
        log.error("Razorpay signature mismatch: orderId={} paymentId={}", req.getRazorpayOrderId(), req.getRazorpayPaymentId());
        throw new IllegalArgumentException("Invalid Razorpay signature – payment rejected.");
      }
    } else {
      log.warn("⚠ Skipping Razorpay signature verification (dev mode). Do NOT use in production!");
    }

    // Activate / upsert subscription
    Instant now = Instant.now();
    Instant end = now.plus(Duration.ofDays(365)); // lifetime → 1 year expiry

    Subscription sub = subscriptionRepository
        .findTopByUserIdOrderByEndDateDesc(req.getUserId())
        .orElseGet(() -> Subscription.builder().userId(req.getUserId()).build());

    sub.setPlanType(PlanType.PREMIUM);
    sub.setStatus(SubscriptionStatus.ACTIVE);
    sub.setRazorpayOrderId(req.getRazorpayOrderId());
    sub.setRazorpayPaymentId(req.getRazorpayPaymentId());
    sub.setStartDate(now);
    sub.setEndDate(end);

    subscriptionRepository.save(sub);
    log.info("✅ Premium activated: userId={} until={}", req.getUserId(), end);
  }

  // ─── GET STATUS ────────────────────────────────────────────────

  @Transactional
  public SubscriptionStatusResponse getSubscriptionStatus(Long userId) {
    Instant now = Instant.now();

    Subscription sub = subscriptionRepository
        .findTopByUserIdOrderByEndDateDesc(userId)
        .orElse(null);

    if (sub == null) {
      return SubscriptionStatusResponse.builder()
          .userId(userId).plan("FREE").status("EXPIRED").build();
    }

    // Auto-expire
    if (sub.getStatus() == SubscriptionStatus.ACTIVE
        && sub.getEndDate() != null
        && sub.getEndDate().isBefore(now)) {
      sub.setStatus(SubscriptionStatus.EXPIRED);
      sub.setPlanType(PlanType.FREE);
      subscriptionRepository.save(sub);
    }

    boolean premiumActive = sub.getPlanType() == PlanType.PREMIUM
                            && sub.getStatus() == SubscriptionStatus.ACTIVE;

    return SubscriptionStatusResponse.builder()
        .userId(userId)
        .plan(premiumActive ? "PREMIUM" : "FREE")
        .status(premiumActive ? "ACTIVE" : "EXPIRED")
        .build();
  }
}
