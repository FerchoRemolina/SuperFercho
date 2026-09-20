package com.superfercho.payments.infrastructure.persistence;

import com.superfercho.payments.application.port.PaymentRepository;
import com.superfercho.payments.domain.model.Payment;
import com.superfercho.payments.infrastructure.persistence.mapper.PaymentPersistenceMapper;
import com.superfercho.payments.infrastructure.persistence.repository.PaymentJpaRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class PaymentPersistenceAdapter implements PaymentRepository {

    private final PaymentJpaRepository paymentJpaRepository;
    private final PaymentPersistenceMapper paymentPersistenceMapper;

    public PaymentPersistenceAdapter(
            PaymentJpaRepository paymentJpaRepository, PaymentPersistenceMapper paymentPersistenceMapper) {
        this.paymentJpaRepository = paymentJpaRepository;
        this.paymentPersistenceMapper = paymentPersistenceMapper;
    }

    @Override
    public Payment save(Payment payment) {
        return paymentPersistenceMapper.toDomain(
                paymentJpaRepository.saveAndFlush(paymentPersistenceMapper.toEntity(payment)));
    }

    @Override
    public Optional<Payment> findById(UUID paymentId) {
        return paymentJpaRepository.findById(paymentId).map(paymentPersistenceMapper::toDomain);
    }
}
