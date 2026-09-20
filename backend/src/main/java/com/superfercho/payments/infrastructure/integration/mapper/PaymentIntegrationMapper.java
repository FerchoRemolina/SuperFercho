package com.superfercho.payments.infrastructure.integration.mapper;

import com.superfercho.orders.application.dto.PaymentRequest;
import com.superfercho.orders.application.dto.PaymentResult;
import com.superfercho.payments.application.dto.PaymentResponse;
import com.superfercho.payments.application.dto.ProcessPaymentCommand;
import com.superfercho.payments.domain.model.PaymentMethod;
import com.superfercho.payments.domain.model.PaymentStatus;

public final class PaymentIntegrationMapper {

    private PaymentIntegrationMapper() {}

    public static ProcessPaymentCommand toCommand(PaymentRequest request) {
        return new ProcessPaymentCommand(
                request.orderId(), request.amount(), toPaymentsPaymentMethod(request.paymentMethod()));
    }

    public static PaymentResult toResult(PaymentResponse response) {
        return new PaymentResult(
                response.paymentId(),
                response.amount(),
                toOrdersPaymentMethod(response.paymentMethod()),
                toOrdersPaymentStatus(response.status()),
                response.providerReference(),
                response.refundedAt(),
                response.createdAt(),
                response.updatedAt());
    }

    public static com.superfercho.orders.application.dto.PaymentMethod toOrdersPaymentMethod(PaymentMethod method) {
        return switch (method) {
            case SIMULATED_CARD -> com.superfercho.orders.application.dto.PaymentMethod.SIMULATED_CARD;
            case CASH_ON_DELIVERY -> com.superfercho.orders.application.dto.PaymentMethod.CASH_ON_DELIVERY;
        };
    }

    public static PaymentMethod toPaymentsPaymentMethod(
            com.superfercho.orders.application.dto.PaymentMethod paymentMethod) {
        return switch (paymentMethod) {
            case SIMULATED_CARD -> PaymentMethod.SIMULATED_CARD;
            case CASH_ON_DELIVERY -> PaymentMethod.CASH_ON_DELIVERY;
        };
    }

    public static com.superfercho.orders.application.dto.PaymentStatus toOrdersPaymentStatus(PaymentStatus status) {
        return switch (status) {
            case PENDING -> com.superfercho.orders.application.dto.PaymentStatus.PENDING;
            case APPROVED -> com.superfercho.orders.application.dto.PaymentStatus.APPROVED;
            case DECLINED -> com.superfercho.orders.application.dto.PaymentStatus.DECLINED;
        };
    }
}
