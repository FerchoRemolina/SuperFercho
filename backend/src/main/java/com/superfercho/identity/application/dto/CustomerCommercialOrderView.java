package com.superfercho.identity.application.dto;

import com.superfercho.platform.money.Money;
import java.time.Instant;
import java.util.UUID;

/** Commercial order snapshot for admin CustomerRecord history (no module-domain leakage). */
public record CustomerCommercialOrderView(
        UUID id,
        String orderNumber,
        UUID customerId,
        String status,
        Money subtotal,
        Money total,
        UUID paymentId,
        Instant createdAt,
        Instant confirmedAt,
        Instant cancelledAt,
        Instant updatedAt) {}
