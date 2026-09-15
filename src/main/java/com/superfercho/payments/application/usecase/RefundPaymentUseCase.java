package com.superfercho.payments.application.usecase;

import com.superfercho.payments.application.dto.PaymentResponse;
import com.superfercho.payments.application.dto.RefundPaymentCommand;
import com.superfercho.payments.application.exception.PaymentNotFoundException;
import com.superfercho.payments.application.port.ClockPort;
import com.superfercho.payments.application.port.PaymentRepository;
import com.superfercho.payments.domain.model.Payment;

public final class RefundPaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final ClockPort clockPort;

    public RefundPaymentUseCase(PaymentRepository paymentRepository, ClockPort clockPort) {
        this.paymentRepository = paymentRepository;
        this.clockPort = clockPort;
    }

    public PaymentResponse execute(RefundPaymentCommand command) {
        Payment payment = paymentRepository
                .findById(command.paymentId())
                .orElseThrow(() -> new PaymentNotFoundException(command.paymentId()));
        Payment refunded = payment.refund(clockPort.currentTime());
        return PaymentResponse.from(paymentRepository.save(refunded));
    }
}
