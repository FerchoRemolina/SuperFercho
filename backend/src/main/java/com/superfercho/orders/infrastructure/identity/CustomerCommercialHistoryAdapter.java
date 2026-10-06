package com.superfercho.orders.infrastructure.identity;

import com.superfercho.identity.application.dto.AdminPagedResult;
import com.superfercho.identity.application.dto.CustomerCommercialOrderView;
import com.superfercho.identity.application.dto.CustomerCommercialPaymentView;
import com.superfercho.identity.application.port.CustomerCommercialHistoryPort;
import com.superfercho.orders.application.dto.PagedResult;
import com.superfercho.orders.application.dto.PaymentResult;
import com.superfercho.orders.application.port.OrderRepository;
import com.superfercho.orders.application.port.PaymentPort;
import com.superfercho.orders.domain.model.Order;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class CustomerCommercialHistoryAdapter implements CustomerCommercialHistoryPort {


    private final OrderRepository orderRepository;
    private final PaymentPort paymentPort;

    public CustomerCommercialHistoryAdapter(OrderRepository orderRepository, PaymentPort paymentPort) {
        this.orderRepository = orderRepository;
        this.paymentPort = paymentPort;
    }

    @Override
    public AdminPagedResult<CustomerCommercialOrderView> findOrdersByCustomerIds(
            Collection<UUID> customerIds, Integer page, Integer size) {
        // PageRequest.of applies defaults/caps; JPA sorts by createdAt DESC, id ASC.
        var pageRequest = com.superfercho.orders.application.dto.PageRequest.of(page, size);
        if (customerIds == null || customerIds.isEmpty()) {
            return AdminPagedResult.empty(pageRequest.page(), pageRequest.size());
        }
        PagedResult<Order> orders = orderRepository.findByCustomerIds(customerIds, pageRequest);
        List<CustomerCommercialOrderView> items =
                orders.items().stream().map(CustomerCommercialHistoryAdapter::toOrderView).toList();
        return new AdminPagedResult<>(items, orders.page(), orders.size(), orders.totalElements());
    }

    @Override
    public AdminPagedResult<CustomerCommercialPaymentView> findPaymentsByCustomerIds(
            Collection<UUID> customerIds, Integer page, Integer size) {
        // Same deterministic order as orders (order.createdAt DESC, id) for stable pages.
        var pageRequest = com.superfercho.orders.application.dto.PageRequest.of(page, size);
        if (customerIds == null || customerIds.isEmpty()) {
            return AdminPagedResult.empty(pageRequest.page(), pageRequest.size());
        }
        PagedResult<Order> orders =
                orderRepository.findOrdersWithPaymentByCustomerIds(customerIds, pageRequest);
        List<CustomerCommercialPaymentView> items = new ArrayList<>(orders.items().size());
        for (Order order : orders.items()) {
            PaymentResult payment = paymentPort.getPayment(order.paymentId());
            items.add(toPaymentView(order.id(), payment));
        }
        return new AdminPagedResult<>(items, orders.page(), orders.size(), orders.totalElements());
    }

    private static CustomerCommercialOrderView toOrderView(Order order) {
        return new CustomerCommercialOrderView(
                order.id(),
                order.orderNumber().value(),
                order.customerId(),
                order.status().name(),
                order.subtotal(),
                order.total(),
                order.paymentId(),
                order.createdAt(),
                order.confirmedAt(),
                order.cancelledAt(),
                order.updatedAt());
    }

    private static CustomerCommercialPaymentView toPaymentView(UUID orderId, PaymentResult payment) {
        return new CustomerCommercialPaymentView(
                payment.paymentId(),
                orderId,
                payment.amount(),
                payment.paymentMethod() == null ? null : payment.paymentMethod().name(),
                payment.status() == null ? null : payment.status().name(),
                payment.providerReference(),
                payment.createdAt(),
                payment.updatedAt(),
                payment.refundedAt());
    }
}
