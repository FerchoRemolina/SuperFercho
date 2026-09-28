package com.superfercho.identity.application.dto;

import com.superfercho.platform.money.Money;
import java.time.Instant;
import java.util.UUID;

/** Commercial payment snapshot for admin CustomerRecord history. */
public record CustomerCommercialPaymentView(
        UUID id,
        UUID orderId,
        Money amount,
        String paymentMethod,
        String status,
        String providerReference,
        Instant createdAt,
        Instant updatedAt,
        Instant refundedAt) {}
