package com.superfercho.payments.domain.model;

import com.superfercho.payments.domain.exception.InvalidPaymentException;
import com.superfercho.platform.money.Money;
import java.time.Instant;
import java.util.UUID;

public final class Payment {

    private final UUID id;
    private final UUID orderId;
    private final Money amount;
    private final PaymentMethod paymentMethod;
    private final PaymentStatus status;
    private final String providerReference;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Instant refundedAt;

    private Payment(
            UUID id,
            UUID orderId,
            Money amount,
            PaymentMethod paymentMethod,
            PaymentStatus status,
            String providerReference,
            Instant createdAt,
            Instant updatedAt,
            Instant refundedAt) {
        this.id = id;
        this.orderId = orderId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.providerReference = providerReference;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.refundedAt = refundedAt;
    }

    public static Payment create(
            UUID id,
            UUID orderId,
            Money amount,
            PaymentMethod paymentMethod,
            PaymentStatus status,
            String providerReference,
            Instant createdAt) {
        return of(id, orderId, amount, paymentMethod, status, providerReference, createdAt, createdAt, null);
    }

    public static Payment reconstitute(
            UUID id,
            UUID orderId,
            Money amount,
            PaymentMethod paymentMethod,
            PaymentStatus status,
            String providerReference,
            Instant createdAt,
            Instant updatedAt,
            Instant refundedAt) {
        return of(
                id,
                orderId,
                amount,
                paymentMethod,
                status,
                providerReference,
                createdAt,
                updatedAt,
                refundedAt);
    }

    public Payment refund(Instant refundedAt) {
        Instant at = requireRefundedAt(refundedAt);
        if (status != PaymentStatus.APPROVED) {
            throw new InvalidPaymentException("payment cannot be refunded unless APPROVED");
        }
        if (this.refundedAt != null) {
            throw new InvalidPaymentException("payment has already been refunded");
        }
        return of(id, orderId, amount, paymentMethod, status, providerReference, createdAt, at, at);
    }

    public UUID id() {
        return id;
    }

    public UUID orderId() {
        return orderId;
    }

    public Money amount() {
        return amount;
    }

    public PaymentMethod paymentMethod() {
        return paymentMethod;
    }

    public PaymentStatus status() {
        return status;
    }

    public String providerReference() {
        return providerReference;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public Instant refundedAt() {
        return refundedAt;
    }

    private Instant requireRefundedAt(Instant refundedAt) {
        requireNonNull(refundedAt, "refundedAt");
        if (refundedAt.isBefore(createdAt)) {
            throw new InvalidPaymentException("refundedAt must not be before createdAt");
        }
        return refundedAt;
    }

    private static Payment of(
            UUID id,
            UUID orderId,
            Money amount,
            PaymentMethod paymentMethod,
            PaymentStatus status,
            String providerReference,
            Instant createdAt,
            Instant updatedAt,
            Instant refundedAt) {
        requireNonNull(id, "id");
        requireNonNull(orderId, "orderId");
        requireNonNull(amount, "amount");
        requireNonNull(paymentMethod, "paymentMethod");
        requireNonNull(status, "status");
        requireNonNull(createdAt, "createdAt");
        requireNonNull(updatedAt, "updatedAt");
        if (createdAt.isAfter(updatedAt)) {
            throw new InvalidPaymentException("createdAt must not be after updatedAt");
        }
        if (refundedAt != null && status != PaymentStatus.APPROVED) {
            throw new InvalidPaymentException("refundedAt can only exist when the payment is APPROVED");
        }
        return new Payment(
                id,
                orderId,
                amount,
                paymentMethod,
                status,
                providerReference,
                createdAt,
                updatedAt,
                refundedAt);
    }

    private static void requireNonNull(Object value, String field) {
        if (value == null) {
            throw new InvalidPaymentException(field + " cannot be null");
        }
    }
}
