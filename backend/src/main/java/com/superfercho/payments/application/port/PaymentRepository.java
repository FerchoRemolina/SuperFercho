package com.superfercho.payments.application.port;

import com.superfercho.payments.domain.model.Payment;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {

    Payment save(Payment payment);

    Optional<Payment> findById(UUID paymentId);

    void delete(UUID paymentId);
}
