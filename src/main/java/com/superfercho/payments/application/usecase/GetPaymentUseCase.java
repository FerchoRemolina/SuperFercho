package com.superfercho.payments.application.usecase;

import com.superfercho.payments.application.dto.GetPaymentCommand;
import com.superfercho.payments.application.dto.PaymentResponse;
import com.superfercho.payments.application.exception.PaymentNotFoundException;
import com.superfercho.payments.application.port.PaymentRepository;

public final class GetPaymentUseCase {

    private final PaymentRepository paymentRepository;

    public GetPaymentUseCase(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public PaymentResponse execute(GetPaymentCommand command) {
        return PaymentResponse.from(paymentRepository
                .findById(command.paymentId())
                .orElseThrow(() -> new PaymentNotFoundException(command.paymentId())));
    }
}
