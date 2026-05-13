package com.payment.razorpay;

import static org.assertj.core.api.Assertions.assertThat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class RazorpaySignatureVerifierTest {

  private RazorpaySignatureVerifier verifier;

  @BeforeEach
  void setUp() {
    verifier = new RazorpaySignatureVerifier();
    ReflectionTestUtils.setField(verifier, "keySecret", "secret");
  }

  @Test
  void verify_WhenSignatureMatches_ShouldReturnTrue() throws Exception {
    String signature = hmacSha256Hex("order_123|pay_123", "secret");

    boolean valid = verifier.verify("order_123", "pay_123", signature);

    assertThat(valid).isTrue();
  }

  @Test
  void verify_WhenSignatureDiffers_ShouldReturnFalse() {
    boolean valid = verifier.verify("order_123", "pay_123", "bad-signature");

    assertThat(valid).isFalse();
  }

  private String hmacSha256Hex(String data, String secret) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    byte[] digest = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    StringBuilder result = new StringBuilder(digest.length * 2);
    for (byte b : digest) {
      result.append(String.format("%02x", b));
    }
    return result.toString();
  }
}
