package com.superfercho.payments.application.dto;

import com.superfercho.payments.domain.model.Payment;
import com.superfercho.payments.domain.model.PaymentMethod;
import com.superfercho.payments.domain.model.PaymentStatus;
import com.superfercho.platform.money.Money;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID paymentId,
        UUID orderId,
        Money amount,
        PaymentMethod paymentMethod,
        PaymentStatus status,
        String providerReference,
        Instant createdAt,
        Instant updatedAt,
        Instant refundedAt) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.id(),
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
