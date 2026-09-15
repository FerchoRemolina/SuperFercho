package com.superfercho.payments.application.usecase;

import com.superfercho.payments.application.dto.PaymentResponse;
import com.superfercho.payments.application.dto.ProcessPaymentCommand;
import com.superfercho.payments.application.port.ClockPort;
import com.superfercho.payments.application.port.PaymentRepository;
import com.superfercho.payments.domain.model.Payment;
import com.superfercho.payments.domain.model.PaymentMethod;
import com.superfercho.payments.domain.model.PaymentStatus;
import java.util.UUID;

public final class ProcessPaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final ClockPort clockPort;

    public ProcessPaymentUseCase(PaymentRepository paymentRepository, ClockPort clockPort) {
        this.paymentRepository = paymentRepository;
        this.clockPort = clockPort;
    }

    public PaymentResponse execute(ProcessPaymentCommand command) {
        Payment payment = Payment.create(
                UUID.randomUUID(),
                command.orderId(),
                command.amount(),
                command.paymentMethod(),
                simulatedStatus(command.paymentMethod()),
                simulatedProviderReference(command.paymentMethod()),
                clockPort.currentTime());
        return PaymentResponse.from(paymentRepository.save(payment));
    }

    private static PaymentStatus simulatedStatus(PaymentMethod paymentMethod) {
        return switch (paymentMethod) {
            case SIMULATED_CARD -> PaymentStatus.APPROVED;
            case CASH_ON_DELIVERY -> PaymentStatus.PENDING;
        };
    }

    private static String simulatedProviderReference(PaymentMethod paymentMethod) {
        return switch (paymentMethod) {
            case SIMULATED_CARD -> "sim-approved";
            case CASH_ON_DELIVERY -> "cod-pending";
        };
    }
}
