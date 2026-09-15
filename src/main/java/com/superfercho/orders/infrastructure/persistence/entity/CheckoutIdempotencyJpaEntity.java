package com.superfercho.orders.infrastructure.persistence.entity;

import com.superfercho.orders.application.dto.PaymentStatus;
import com.superfercho.orders.domain.model.OrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "checkout_idempotency", schema = "orders")
public class CheckoutIdempotencyJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "fingerprint", nullable = false)
    private String fingerprint;

    @Column(name = "result_order_id", nullable = false)
    private UUID resultOrderId;

    @Column(name = "result_order_number", nullable = false)
    private String resultOrderNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_order_status", nullable = false)
    private OrderStatus resultOrderStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_payment_status", nullable = false)
    private PaymentStatus resultPaymentStatus;

    @Column(name = "result_total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal resultTotalAmount;

    @Column(name = "result_total_currency", nullable = false, length = 3)
    private String resultTotalCurrency;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected CheckoutIdempotencyJpaEntity() {
    }

    public CheckoutIdempotencyJpaEntity(
            UUID id,
            UUID customerId,
            String idempotencyKey,
            String fingerprint,
            UUID resultOrderId,
            String resultOrderNumber,
            OrderStatus resultOrderStatus,
            PaymentStatus resultPaymentStatus,
            BigDecimal resultTotalAmount,
            String resultTotalCurrency,
            Instant createdAt,
            Instant expiresAt) {
        this.id = id;
        this.customerId = customerId;
        this.idempotencyKey = idempotencyKey;
        this.fingerprint = fingerprint;
        this.resultOrderId = resultOrderId;
        this.resultOrderNumber = resultOrderNumber;
        this.resultOrderStatus = resultOrderStatus;
        this.resultPaymentStatus = resultPaymentStatus;
        this.resultTotalAmount = resultTotalAmount;
        this.resultTotalCurrency = resultTotalCurrency;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public UUID getResultOrderId() {
        return resultOrderId;
    }

    public String getResultOrderNumber() {
        return resultOrderNumber;
    }

    public OrderStatus getResultOrderStatus() {
        return resultOrderStatus;
    }

    public PaymentStatus getResultPaymentStatus() {
        return resultPaymentStatus;
    }

    public BigDecimal getResultTotalAmount() {
        return resultTotalAmount;
    }

    public String getResultTotalCurrency() {
        return resultTotalCurrency;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
