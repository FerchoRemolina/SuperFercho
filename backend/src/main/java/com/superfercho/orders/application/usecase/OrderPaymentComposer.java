package com.superfercho.orders.application.usecase;

import com.superfercho.orders.application.dto.OrderResult;
import com.superfercho.orders.application.dto.PaymentResult;
import com.superfercho.orders.application.port.PaymentPort;
import com.superfercho.orders.domain.model.Order;
import com.superfercho.payments.application.exception.PaymentNotFoundException;

final class OrderPaymentComposer {

    private OrderPaymentComposer() {}

    static OrderResult compose(Order order, PaymentPort paymentPort) {
        return OrderResult.from(order, payment(order, paymentPort));
    }

    /**
     * Admin variant: a dangling paymentId degrades to {@code payment = null}
     * instead of failing the whole order detail. Customer flow stays strict.
     */
    static OrderResult composeToleratingMissingPayment(Order order, PaymentPort paymentPort) {
        PaymentResult payment;
        try {
            payment = payment(order, paymentPort);
        } catch (PaymentNotFoundException exception) {
            payment = null;
        }
        return OrderResult.from(order, payment);
    }

    private static PaymentResult payment(Order order, PaymentPort paymentPort) {
        if (order.paymentId() == null) {
            return null;
        }
        return paymentPort.getPayment(order.paymentId());
    }
}
