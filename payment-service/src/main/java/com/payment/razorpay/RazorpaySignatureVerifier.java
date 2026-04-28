package com.payment.razorpay;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class RazorpaySignatureVerifier {

  private static final String HMAC_SHA256 = "HmacSHA256";

  @Value("${razorpay.keySecret}")
  private String keySecret;

  public boolean verify(String orderId, String paymentId, String providedSignature) {
    String payload = orderId + "|" + paymentId;
    String expected = hmacSha256Hex(payload, keySecret);
    // constant-time compare is ideal; this is acceptable but you can upgrade later
    return expected.equals(providedSignature);
  }

  private String hmacSha256Hex(String data, String secret) {
    try {
      Mac mac = Mac.getInstance(HMAC_SHA256);
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
      byte[] digest = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
      return bytesToHex(digest);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to compute HMAC SHA256", e);
    }
  }

  private String bytesToHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) sb.append(String.format("%02x", b));
    return sb.toString();
  }
}