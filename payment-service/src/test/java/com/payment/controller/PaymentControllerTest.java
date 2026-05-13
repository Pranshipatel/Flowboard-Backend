package com.payment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.payment.dto.CreateOrderRequest;
import com.payment.dto.CreateOrderResponse;
import com.payment.dto.VerifyPaymentRequest;
import com.payment.service.PaymentSubscriptionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

  @Mock
  private PaymentSubscriptionService paymentService;

  @Test
  void createOrder_ShouldDelegateToService() {
    PaymentController controller = new PaymentController(paymentService);
    CreateOrderRequest request = new CreateOrderRequest();
    request.setUserId(7L);
    request.setAmount(499L);
    CreateOrderResponse serviceResponse = CreateOrderResponse.builder()
        .razorpayOrderId("order_7")
        .amount(49900L)
        .currency("INR")
        .keyId("key")
        .build();
    when(paymentService.createOrder(7L, 499L)).thenReturn(serviceResponse);

    ResponseEntity<CreateOrderResponse> response = controller.createOrder(request);

    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(response.getBody()).isSameAs(serviceResponse);
  }

  @Test
  void verify_ShouldDelegateToService() {
    PaymentController controller = new PaymentController(paymentService);
    VerifyPaymentRequest request = new VerifyPaymentRequest();

    ResponseEntity<Void> response = controller.verify(request);

    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    verify(paymentService).verifyAndActivatePremium(request);
  }
}
