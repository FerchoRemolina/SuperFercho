package com.superfercho.orders.infrastructure.persistence.mapper;

import com.superfercho.orders.application.dto.CheckoutRequestFingerprint;
import com.superfercho.orders.application.dto.CheckoutResult;
import com.superfercho.orders.application.dto.IdempotencyRecord;
import com.superfercho.orders.infrastructure.persistence.entity.CheckoutIdempotencyJpaEntity;
import com.superfercho.platform.money.Money;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CheckoutIdempotencyPersistenceMapper {

    public CheckoutIdempotencyJpaEntity toEntity(UUID id, IdempotencyRecord record) {
        CheckoutResult result = record.result();
        return new CheckoutIdempotencyJpaEntity(
                id,
                record.customerId(),
                record.key(),
                record.fingerprint().value(),
                result.orderId(),
                result.orderNumber(),
                result.status(),
                result.paymentStatus(),
                result.total().amount(),
                result.total().currency(),
                record.createdAt(),
                record.expiresAt());
    }

    public IdempotencyRecord toRecord(CheckoutIdempotencyJpaEntity entity) {
        return new IdempotencyRecord(
                entity.getIdempotencyKey(),
                entity.getCustomerId(),
                new CheckoutRequestFingerprint(entity.getFingerprint()),
                new CheckoutResult(
                        entity.getResultOrderId(),
                        entity.getResultOrderNumber(),
                        entity.getResultOrderStatus(),
                        entity.getResultPaymentStatus(),
                        new Money(entity.getResultTotalAmount(), entity.getResultTotalCurrency())),
                entity.getCreatedAt(),
                entity.getExpiresAt());
    }
}
