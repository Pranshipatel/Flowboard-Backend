package com.payment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.payment.dto.SubscriptionStatusResponse;
import com.payment.service.PaymentSubscriptionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class SubscriptionControllerTest {

  @Mock
  private PaymentSubscriptionService paymentService;

  @Test
  void status_ShouldReturnServiceResponse() {
    SubscriptionController controller = new SubscriptionController(paymentService);
    SubscriptionStatusResponse serviceResponse = SubscriptionStatusResponse.builder()
        .userId(7L)
        .plan("PREMIUM")
        .status("ACTIVE")
        .build();
    when(paymentService.getSubscriptionStatus(7L)).thenReturn(serviceResponse);

    ResponseEntity<SubscriptionStatusResponse> response = controller.status(7L);

    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(response.getBody()).isSameAs(serviceResponse);
  }
}
