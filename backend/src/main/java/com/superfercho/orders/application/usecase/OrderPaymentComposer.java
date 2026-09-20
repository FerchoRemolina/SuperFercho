package com.superfercho.orders.application.usecase;

import com.superfercho.orders.application.dto.OrderResult;
import com.superfercho.orders.application.dto.PaymentResult;
import com.superfercho.orders.application.port.PaymentPort;
import com.superfercho.orders.domain.model.Order;

final class OrderPaymentComposer {

    private OrderPaymentComposer() {}

    static OrderResult compose(Order order, PaymentPort paymentPort) {
        PaymentResult payment = null;
        if (order.paymentId() != null) {
            payment = paymentPort.getPayment(order.paymentId());
        }
        return OrderResult.from(order, payment);
    }
}
