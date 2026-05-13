package com.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.razorpay.OrderClient;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import java.time.Instant;
import java.util.Optional;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PaymentSubscriptionServiceTest {

  @Mock
  private RazorpayClient razorpayClient;

  @Mock
  private OrderClient orderClient;

  @Mock
  private RazorpaySignatureVerifier signatureVerifier;

  @Mock
  private SubscriptionRepository subscriptionRepository;

  private PaymentSubscriptionService service;

  @BeforeEach
  void setUp() {
    razorpayClient.orders = orderClient;
    service = new PaymentSubscriptionService(razorpayClient, signatureVerifier, subscriptionRepository);
    ReflectionTestUtils.setField(service, "razorpayKeyId", "rzp_test_key");
    ReflectionTestUtils.setField(service, "skipVerification", false);
  }

  @Test
  void createOrder_WhenRazorpaySucceeds_ShouldReturnGatewayValues() throws Exception {
    Order order = new Order(new JSONObject()
        .put("id", "order_123")
        .put("amount", 50000)
        .put("currency", "INR"));
    when(orderClient.create(any(JSONObject.class))).thenReturn(order);

    CreateOrderResponse response = service.createOrder(10L, 500L);

    assertThat(response.getRazorpayOrderId()).isEqualTo("order_123");
    assertThat(response.getAmount()).isEqualTo(50000L);
    assertThat(response.getCurrency()).isEqualTo("INR");
    assertThat(response.getKeyId()).isEqualTo("rzp_test_key");
  }

  @Test
  void createOrder_WhenRazorpayFails_ShouldThrowDedicatedException() throws Exception {
    when(orderClient.create(any(JSONObject.class))).thenThrow(new RazorpayException("gateway down"));

    assertThatThrownBy(() -> service.createOrder(10L, 500L))
        .isInstanceOf(PaymentProcessingException.class)
        .hasMessageContaining("Failed to create Razorpay order");
  }

  @Test
  void verifyAndActivatePremium_WhenPaymentAlreadyProcessed_ShouldReturnWithoutSaving() {
    VerifyPaymentRequest request = paymentRequest();
    when(subscriptionRepository.findByRazorpayPaymentId("pay_123"))
        .thenReturn(Optional.of(Subscription.builder().id(99L).build()));

    service.verifyAndActivatePremium(request);

    verify(signatureVerifier, never()).verify(any(), any(), any());
    verify(subscriptionRepository, never()).save(any());
  }

  @Test
  void verifyAndActivatePremium_WhenSignatureInvalid_ShouldRejectPayment() {
    VerifyPaymentRequest request = paymentRequest();
    when(subscriptionRepository.findByRazorpayPaymentId("pay_123")).thenReturn(Optional.empty());
    when(signatureVerifier.verify("order_123", "pay_123", "signature")).thenReturn(false);

    assertThatThrownBy(() -> service.verifyAndActivatePremium(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid Razorpay signature");

    verify(subscriptionRepository, never()).save(any());
  }

  @Test
  void verifyAndActivatePremium_WhenNewPremiumPaymentIsValid_ShouldCreateSubscription() {
    VerifyPaymentRequest request = paymentRequest();
    when(subscriptionRepository.findByRazorpayPaymentId("pay_123")).thenReturn(Optional.empty());
    when(signatureVerifier.verify("order_123", "pay_123", "signature")).thenReturn(true);
    when(subscriptionRepository.findTopByUserIdOrderByEndDateDesc(42L)).thenReturn(Optional.empty());

    service.verifyAndActivatePremium(request);

    ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
    verify(subscriptionRepository).save(captor.capture());
    Subscription saved = captor.getValue();
    assertThat(saved.getUserId()).isEqualTo(42L);
    assertThat(saved.getPlanType()).isEqualTo(PlanType.PREMIUM);
    assertThat(saved.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    assertThat(saved.getRazorpayOrderId()).isEqualTo("order_123");
    assertThat(saved.getRazorpayPaymentId()).isEqualTo("pay_123");
    assertThat(saved.getEndDate()).isAfter(saved.getStartDate());
  }

  @Test
  void verifyAndActivatePremium_WhenSkipVerificationIsEnabled_ShouldSaveWithoutVerifier() {
    ReflectionTestUtils.setField(service, "skipVerification", true);
    VerifyPaymentRequest request = paymentRequest();
    Subscription existing = Subscription.builder().userId(42L).planType(PlanType.FREE).status(SubscriptionStatus.EXPIRED).build();
    when(subscriptionRepository.findByRazorpayPaymentId("pay_123")).thenReturn(Optional.empty());
    when(subscriptionRepository.findTopByUserIdOrderByEndDateDesc(42L)).thenReturn(Optional.of(existing));

    service.verifyAndActivatePremium(request);

    verify(signatureVerifier, never()).verify(any(), any(), any());
    verify(subscriptionRepository).save(existing);
    assertThat(existing.getPlanType()).isEqualTo(PlanType.PREMIUM);
    assertThat(existing.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
  }

  @Test
  void getSubscriptionStatus_WhenNoSubscriptionExists_ShouldReturnFreeExpired() {
    when(subscriptionRepository.findTopByUserIdOrderByEndDateDesc(42L)).thenReturn(Optional.empty());

    SubscriptionStatusResponse response = service.getSubscriptionStatus(42L);

    assertThat(response.getUserId()).isEqualTo(42L);
    assertThat(response.getPlan()).isEqualTo("FREE");
    assertThat(response.getStatus()).isEqualTo("EXPIRED");
  }

  @Test
  void getSubscriptionStatus_WhenPremiumIsActive_ShouldReturnPremiumActive() {
    Subscription subscription = Subscription.builder()
        .userId(42L)
        .planType(PlanType.PREMIUM)
        .status(SubscriptionStatus.ACTIVE)
        .endDate(Instant.now().plusSeconds(3600))
        .build();
    when(subscriptionRepository.findTopByUserIdOrderByEndDateDesc(42L)).thenReturn(Optional.of(subscription));

    SubscriptionStatusResponse response = service.getSubscriptionStatus(42L);

    assertThat(response.getPlan()).isEqualTo("PREMIUM");
    assertThat(response.getStatus()).isEqualTo("ACTIVE");
    verify(subscriptionRepository, never()).save(any());
  }

  @Test
  void getSubscriptionStatus_WhenActiveSubscriptionExpired_ShouldDowngradeAndReturnFree() {
    Subscription subscription = Subscription.builder()
        .userId(42L)
        .planType(PlanType.PREMIUM)
        .status(SubscriptionStatus.ACTIVE)
        .endDate(Instant.now().minusSeconds(1))
        .build();
    when(subscriptionRepository.findTopByUserIdOrderByEndDateDesc(42L)).thenReturn(Optional.of(subscription));

    SubscriptionStatusResponse response = service.getSubscriptionStatus(42L);

    assertThat(response.getPlan()).isEqualTo("FREE");
    assertThat(response.getStatus()).isEqualTo("EXPIRED");
    assertThat(subscription.getPlanType()).isEqualTo(PlanType.FREE);
    assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
    verify(subscriptionRepository).save(subscription);
  }

  private VerifyPaymentRequest paymentRequest() {
    VerifyPaymentRequest request = new VerifyPaymentRequest();
    request.setUserId(42L);
    request.setRazorpayOrderId("order_123");
    request.setRazorpayPaymentId("pay_123");
    request.setRazorpaySignature("signature");
    return request;
  }
}
