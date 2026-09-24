package com.superfercho.payments.infrastructure.integration;

import com.superfercho.orders.application.dto.PaymentRequest;
import com.superfercho.orders.application.dto.PaymentResult;
import com.superfercho.orders.application.port.PaymentPort;
import com.superfercho.payments.application.dto.DeletePaymentCommand;
import com.superfercho.payments.application.dto.GetPaymentCommand;
import com.superfercho.payments.application.dto.RefundPaymentCommand;
import com.superfercho.payments.application.usecase.DeletePaymentUseCase;
import com.superfercho.payments.application.usecase.GetPaymentUseCase;
import com.superfercho.payments.application.usecase.ProcessPaymentUseCase;
import com.superfercho.payments.application.usecase.RefundPaymentUseCase;
import com.superfercho.payments.infrastructure.integration.mapper.PaymentIntegrationMapper;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class PaymentIntegrationAdapter implements PaymentPort {

    private final ProcessPaymentUseCase processPaymentUseCase;
    private final GetPaymentUseCase getPaymentUseCase;
    private final RefundPaymentUseCase refundPaymentUseCase;
    private final DeletePaymentUseCase deletePaymentUseCase;

    public PaymentIntegrationAdapter(
            ProcessPaymentUseCase processPaymentUseCase,
            GetPaymentUseCase getPaymentUseCase,
            RefundPaymentUseCase refundPaymentUseCase,
            DeletePaymentUseCase deletePaymentUseCase) {
        this.processPaymentUseCase = processPaymentUseCase;
        this.getPaymentUseCase = getPaymentUseCase;
        this.refundPaymentUseCase = refundPaymentUseCase;
        this.deletePaymentUseCase = deletePaymentUseCase;
    }

    @Override
    public PaymentResult processPayment(PaymentRequest request) {
        return PaymentIntegrationMapper.toResult(
                processPaymentUseCase.execute(PaymentIntegrationMapper.toCommand(request)));
    }

    @Override
    public PaymentResult getPayment(UUID paymentId) {
        return PaymentIntegrationMapper.toResult(getPaymentUseCase.execute(new GetPaymentCommand(paymentId)));
    }

    @Override
    public void refundPayment(UUID paymentId) {
        refundPaymentUseCase.execute(new RefundPaymentCommand(paymentId));
    }

    @Override
    public void deletePayment(UUID paymentId) {
        deletePaymentUseCase.execute(new DeletePaymentCommand(paymentId));
    }
}
