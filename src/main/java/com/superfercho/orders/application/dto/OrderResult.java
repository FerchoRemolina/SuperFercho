package com.superfercho.orders.application.dto;

import com.superfercho.orders.domain.model.Order;
import com.superfercho.orders.domain.model.OrderStatus;
import com.superfercho.orders.domain.model.ShippingAddressSnapshot;
import com.superfercho.platform.money.Money;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResult(
        UUID id,
        String orderNumber,
        OrderStatus status,
        List<OrderItemResult> items,
        Money subtotal,
        Money total,
        ShippingAddressResult shippingAddress,
        UUID paymentId,
        Instant createdAt,
        Instant confirmedAt,
        Instant cancelledAt,
        Instant updatedAt) {

    public static OrderResult from(Order order) {
        ShippingAddressSnapshot address = order.shippingAddress();
        return new OrderResult(
                order.id(),
                order.orderNumber().value(),
                order.status(),
                order.items().stream()
                        .map(item -> new OrderItemResult(
                                item.id(),
                                item.productId(),
                                item.productName(),
                                item.unitPrice(),
                                item.quantity(),
                                item.subtotal()))
                        .toList(),
                order.subtotal(),
                order.total(),
                new ShippingAddressResult(
                        address.recipientName(),
                        address.addressLine(),
                        address.additionalInfo(),
                        address.city(),
                        address.department(),
                        address.phone()),
                order.paymentId(),
                order.createdAt(),
                order.confirmedAt(),
                order.cancelledAt(),
                order.updatedAt());
    }
}
