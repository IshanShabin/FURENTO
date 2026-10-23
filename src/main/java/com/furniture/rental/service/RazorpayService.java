package com.furniture.rental.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Service
public class RazorpayService {

    private final String keyId;
    private final String keySecret;
    private final RestClient restClient;

    public RazorpayService(@Value("${razorpay.key-id:}") String keyId,
            @Value("${razorpay.key-secret:}") String keySecret,
            RestClient.Builder restClientBuilder) {
        this.keyId = keyId;
        this.keySecret = keySecret;
        this.restClient = restClientBuilder
                .baseUrl("https://api.razorpay.com/v1")
                .defaultHeaders(headers -> headers.setBasicAuth(keyId, keySecret))
                .build();
    }

    public boolean isConfigured() {
        return !keyId.isBlank() && !keySecret.isBlank();
    }

    public String getKeyId() {
        return keyId;
    }

    public GatewayOrder createOrder(long amountPaise) {
        requireConfigured();
        if (amountPaise <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }

        JsonNode response = restClient.post()
                .uri("/orders")
                .body(Map.of(
                        "amount", amountPaise,
                        "currency", "INR",
                        "receipt", "rent-" + UUID.randomUUID().toString().replace("-", "")))
                .retrieve()
                .body(JsonNode.class);

        if (response == null || response.path("id").isMissingNode()) {
            throw new IllegalStateException("Razorpay did not return an order ID");
        }
        return new GatewayOrder(response.path("id").asText(), response.path("amount").asLong(),
                response.path("currency").asText());
    }

    public boolean verifySignature(String orderId, String paymentId, String signature) {
        requireConfigured();
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal((orderId + "|" + paymentId).getBytes(StandardCharsets.UTF_8));
            byte[] expected = HexFormat.of().formatHex(digest).getBytes(StandardCharsets.US_ASCII);
            return MessageDigest.isEqual(expected, signature.getBytes(StandardCharsets.US_ASCII));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not verify the Razorpay payment signature", exception);
        }
    }

    public GatewayPayment fetchPayment(String paymentId) {
        requireConfigured();
        JsonNode response = restClient.get()
                .uri("/payments/{paymentId}", paymentId)
                .retrieve()
                .body(JsonNode.class);

        if (response == null) {
            throw new IllegalStateException("Razorpay did not return payment details");
        }
        return new GatewayPayment(response.path("order_id").asText(), response.path("status").asText(),
                response.path("method").asText(), response.path("amount").asLong());
    }

    public GatewayPayment capturePayment(String paymentId, long amountPaise) {
        requireConfigured();
        JsonNode response = restClient.post()
                .uri("/payments/{paymentId}/capture", paymentId)
                .body(Map.of("amount", amountPaise, "currency", "INR"))
                .retrieve()
                .body(JsonNode.class);

        if (response == null) {
            throw new IllegalStateException("Razorpay did not return captured payment details");
        }
        return new GatewayPayment(response.path("order_id").asText(), response.path("status").asText(),
                response.path("method").asText(), response.path("amount").asLong());
    }

    public static long toPaise(BigDecimal amount) {
        return amount.movePointRight(2).longValueExact();
    }

    private void requireConfigured() {
        if (!isConfigured()) {
            throw new IllegalStateException("Razorpay keys are not configured");
        }
    }

    public record GatewayOrder(String id, long amountPaise, String currency) {
    }

    public record GatewayPayment(String orderId, String status, String method, long amountPaise) {
    }
}