package com.superfercho.orders.application.dto;

import java.time.Instant;
import java.util.UUID;

public record IdempotencyRecord(
        String key,
        UUID customerId,
        CheckoutRequestFingerprint fingerprint,
        CheckoutResult result,
        Instant createdAt,
        Instant expiresAt) {
}
