package com.superfercho.orders.infrastructure.rest.dto;

import com.superfercho.orders.application.dto.PaymentMethod;
import com.superfercho.orders.application.dto.PaymentResult;
import com.superfercho.orders.application.dto.PaymentStatus;
import com.superfercho.platform.money.Money;
import java.time.Instant;
import java.util.UUID;

public record OrderPaymentRestResponse(
        UUID paymentId,
        Money amount,
        PaymentMethod paymentMethod,
        PaymentStatus status,
        String providerReference,
        Instant refundedAt,
        Instant createdAt,
        Instant updatedAt) {

    public static OrderPaymentRestResponse from(PaymentResult payment) {
        if (payment == null) {
            return null;
        }
        return new OrderPaymentRestResponse(
                payment.paymentId(),
                payment.amount(),
                payment.paymentMethod(),
                payment.status(),
                payment.providerReference(),
                payment.refundedAt(),
                payment.createdAt(),
                payment.updatedAt());
    }
}
