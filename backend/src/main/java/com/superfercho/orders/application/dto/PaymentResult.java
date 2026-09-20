package com.superfercho.orders.application.dto;

import com.superfercho.platform.money.Money;
import java.time.Instant;
import java.util.UUID;

public record PaymentResult(
        UUID paymentId,
        Money amount,
        PaymentMethod paymentMethod,
        PaymentStatus status,
        String providerReference,
        Instant refundedAt,
        Instant createdAt,
        Instant updatedAt) {}
