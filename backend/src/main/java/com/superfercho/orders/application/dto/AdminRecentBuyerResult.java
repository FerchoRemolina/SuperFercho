package com.superfercho.orders.application.dto;

import com.superfercho.platform.money.Money;
import java.time.Instant;
import java.util.UUID;

public record AdminRecentBuyerResult(
        UUID customerId,
        String displayName,
        Instant lastOrderAt,
        long orderCount,
        Money lastOrderTotal) {}
