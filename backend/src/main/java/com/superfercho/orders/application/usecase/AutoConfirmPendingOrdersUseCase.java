package com.superfercho.orders.application.usecase;

import com.superfercho.orders.application.dto.OrderResult;
import com.superfercho.orders.application.port.ClockProvider;
import com.superfercho.orders.application.port.OrderRepository;
import com.superfercho.orders.application.port.PreviewCustomerExclusionPort;
import com.superfercho.orders.domain.model.Order;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class AutoConfirmPendingOrdersUseCase {

    private final OrderRepository orderRepository;
    private final ClockProvider clockProvider;
    private final PreviewCustomerExclusionPort previewCustomerExclusionPort;

    public AutoConfirmPendingOrdersUseCase(
            OrderRepository orderRepository,
            ClockProvider clockProvider,
            PreviewCustomerExclusionPort previewCustomerExclusionPort) {
        this.orderRepository = orderRepository;
        this.clockProvider = clockProvider;
        this.previewCustomerExclusionPort = previewCustomerExclusionPort;
    }

    public List<OrderResult> execute() {
        Instant now = clockProvider.currentTime();
        List<OrderResult> confirmed = new ArrayList<>();
        for (Order order : orderRepository.findPendingOrdersEligibleForAutomaticConfirmation(now)) {
            if (previewCustomerExclusionPort.isPreviewTemporaryCustomer(order.customerId())) {
                continue;
            }
            if (order.isEligibleForAutomaticConfirmation(now)) {
                orderRepository
                        .saveIfPending(order.confirm(now))
                        .ifPresent(saved -> confirmed.add(OrderResult.from(saved)));
            }
        }
        return List.copyOf(confirmed);
    }
}
