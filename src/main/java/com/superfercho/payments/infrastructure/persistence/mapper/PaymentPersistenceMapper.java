package com.superfercho.payments.infrastructure.persistence.mapper;

import com.superfercho.payments.domain.model.Payment;
import com.superfercho.payments.infrastructure.persistence.entity.PaymentJpaEntity;
import com.superfercho.platform.money.Money;
import org.springframework.stereotype.Component;

@Component
public class PaymentPersistenceMapper {

    public PaymentJpaEntity toEntity(Payment payment) {
        return new PaymentJpaEntity(
                payment.id(),
                payment.orderId(),
                payment.amount().amount(),
                payment.amount().currency(),
                payment.paymentMethod(),
                payment.status(),
                payment.providerReference(),
                payment.createdAt(),
                payment.updatedAt(),
                payment.refundedAt());
    }

    public Payment toDomain(PaymentJpaEntity entity) {
        return Payment.reconstitute(
                entity.getId(),
                entity.getOrderId(),
                new Money(entity.getAmount(), entity.getCurrency()),
                entity.getPaymentMethod(),
                entity.getStatus(),
                entity.getProviderReference(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getRefundedAt());
    }
}
