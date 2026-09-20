package com.superfercho.orders.application.dto;

import java.util.UUID;

public record PaymentResult(UUID paymentId, PaymentStatus status, String providerReference) {
}
