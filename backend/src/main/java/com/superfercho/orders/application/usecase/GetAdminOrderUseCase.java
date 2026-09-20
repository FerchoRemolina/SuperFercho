package com.superfercho.orders.application.usecase;

import com.superfercho.orders.application.dto.GetOrderCommand;
import com.superfercho.orders.application.dto.OrderResult;
import com.superfercho.orders.application.exception.OrderNotFoundException;
import com.superfercho.orders.application.port.OrderRepository;
import com.superfercho.orders.application.port.PaymentPort;

public final class GetAdminOrderUseCase {

    private final OrderRepository orderRepository;
    private final PaymentPort paymentPort;

    public GetAdminOrderUseCase(OrderRepository orderRepository, PaymentPort paymentPort) {
        this.orderRepository = orderRepository;
        this.paymentPort = paymentPort;
    }

    public OrderResult execute(GetOrderCommand command) {
        return OrderPaymentComposer.compose(
                orderRepository
                        .findById(command.orderId())
                        .orElseThrow(() -> new OrderNotFoundException(command.orderId())),
                paymentPort);
    }
}
