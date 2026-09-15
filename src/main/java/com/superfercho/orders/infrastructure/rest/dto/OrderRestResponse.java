package com.superfercho.orders.infrastructure.rest.dto;

import com.superfercho.orders.application.dto.OrderResult;
import com.superfercho.orders.domain.model.OrderStatus;
import com.superfercho.platform.money.Money;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderRestResponse(
        UUID id,
        String orderNumber,
        OrderStatus status,
        List<OrderItemRestResponse> items,
        Money subtotal,
        Money total,
        ShippingAddressRestResponse shippingAddress,
        UUID paymentId,
        Instant createdAt,
        Instant confirmedAt,
        Instant cancelledAt,
        Instant updatedAt) {

    public static OrderRestResponse from(OrderResult order) {
        return new OrderRestResponse(
                order.id(),
                order.orderNumber(),
                order.status(),
                order.items().stream().map(OrderItemRestResponse::from).toList(),
                order.subtotal(),
                order.total(),
                ShippingAddressRestResponse.from(order.shippingAddress()),
                order.paymentId(),
                order.createdAt(),
                order.confirmedAt(),
                order.cancelledAt(),
                order.updatedAt());
    }
}
