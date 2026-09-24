package com.superfercho.payments.application.usecase;

import com.superfercho.payments.application.dto.DeletePaymentCommand;
import com.superfercho.payments.application.port.PaymentRepository;

public final class DeletePaymentUseCase {

    private final PaymentRepository paymentRepository;

    public DeletePaymentUseCase(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public void execute(DeletePaymentCommand command) {
        paymentRepository.findById(command.paymentId()).ifPresent(payment -> paymentRepository.delete(payment.id()));
    }
}
