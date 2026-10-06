package com.superfercho.orders.application.usecase;

import com.superfercho.orders.application.dto.AdminOrderDetailResult;
import com.superfercho.orders.application.dto.CustomerDirectoryEntry;
import com.superfercho.orders.application.dto.GetOrderCommand;
import com.superfercho.orders.application.exception.OrderNotFoundException;
import com.superfercho.orders.application.port.CustomerDirectoryPort;
import com.superfercho.orders.application.port.OrderRepository;
import com.superfercho.orders.application.port.PaymentPort;
import com.superfercho.orders.domain.model.Order;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Admin order detail. The payment is composed tolerantly (a dangling paymentId
 * yields {@code payment = null}) and the customer is resolved through the
 * {@link CustomerDirectoryPort} with a single batch call of one id.
 */
public final class GetAdminOrderUseCase {

    private final OrderRepository orderRepository;
    private final PaymentPort paymentPort;
    private final CustomerDirectoryPort customerDirectoryPort;

    public GetAdminOrderUseCase(
            OrderRepository orderRepository,
            PaymentPort paymentPort,
            CustomerDirectoryPort customerDirectoryPort) {
        this.orderRepository = orderRepository;
        this.paymentPort = paymentPort;
        this.customerDirectoryPort = customerDirectoryPort;
    }

    public AdminOrderDetailResult execute(GetOrderCommand command) {
        Objects.requireNonNull(command, "command");
        Order order = orderRepository
                .findById(command.orderId())
                .orElseThrow(() -> new OrderNotFoundException(command.orderId()));
        Map<UUID, CustomerDirectoryEntry> customers =
                customerDirectoryPort.findByUserIds(List.of(order.customerId()));
        return new AdminOrderDetailResult(
                OrderPaymentComposer.composeToleratingMissingPayment(order, paymentPort),
                customers.get(order.customerId()));
    }
}
