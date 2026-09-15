package com.superfercho.orders.application.dto;

import java.util.List;
import java.util.UUID;

public record AvailabilityResult(boolean allAvailable, List<UUID> unavailableProductIds) {

    public AvailabilityResult {
        unavailableProductIds = List.copyOf(unavailableProductIds);
    }

    public static AvailabilityResult available() {
        return new AvailabilityResult(true, List.of());
    }

    public static AvailabilityResult unavailable(List<UUID> unavailableProductIds) {
        return new AvailabilityResult(false, unavailableProductIds);
    }
}
