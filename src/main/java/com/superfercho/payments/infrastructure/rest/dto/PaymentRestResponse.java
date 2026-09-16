package com.superfercho.payments.infrastructure.rest.dto;

import com.superfercho.payments.application.dto.PaymentResponse;
import com.superfercho.payments.domain.model.PaymentMethod;
import com.superfercho.payments.domain.model.PaymentStatus;
import com.superfercho.platform.money.Money;
import java.time.Instant;
import java.util.UUID;

public record PaymentRestResponse(
        UUID id,
        UUID orderId,
        Money amount,
        PaymentMethod paymentMethod,
        PaymentStatus status,
        String providerReference,
        Instant createdAt,
        Instant updatedAt,
        Instant refundedAt) {

    public static PaymentRestResponse from(PaymentResponse payment) {
        return new PaymentRestResponse(
                payment.paymentId(),
                payment.orderId(),
                payment.amount(),
                payment.paymentMethod(),
                payment.status(),
                payment.providerReference(),
                payment.createdAt(),
                payment.updatedAt(),
                payment.refundedAt());
    }
}
