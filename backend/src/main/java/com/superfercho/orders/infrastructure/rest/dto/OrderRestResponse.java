package com.superfercho.orders.infrastructure.rest.dto;

import com.superfercho.orders.application.dto.AdminOrderDetailResult;
import com.superfercho.orders.application.dto.OrderResult;
import com.superfercho.orders.application.dto.CustomerDirectoryEntry;
import com.superfercho.orders.domain.model.OrderStatus;
import com.superfercho.platform.money.Money;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Order detail payload. {@code customer} is the enriched commercial identity
 * resolved by the admin flow; it is {@code null} on the customer-facing
 * endpoint and when the account cannot be resolved.
 */
public record OrderRestResponse(
        UUID id,
        String orderNumber,
        UUID customerId,
        OrderStatus status,
        List<OrderItemRestResponse> items,
        Money subtotal,
        Money total,
        ShippingAddressRestResponse shippingAddress,
        UUID paymentId,
        Instant createdAt,
        Instant confirmedAt,
        Instant cancelledAt,
        Instant updatedAt,
        OrderPaymentRestResponse payment,
        AdminOrderCustomerRestResponse customer) {

    public static OrderRestResponse from(OrderResult order) {
        return from(order, null);
    }

    public static OrderRestResponse from(OrderResult order, CustomerDirectoryEntry customer) {
        return new OrderRestResponse(
                order.id(),
                order.orderNumber(),
                order.customerId(),
                order.status(),
                order.items().stream().map(OrderItemRestResponse::from).toList(),
                order.subtotal(),
                order.total(),
                ShippingAddressRestResponse.from(order.shippingAddress()),
                order.paymentId(),
                order.createdAt(),
                order.confirmedAt(),
                order.cancelledAt(),
                order.updatedAt(),
                OrderPaymentRestResponse.from(order.payment()),
                customer == null ? null : AdminOrderCustomerRestResponse.from(customer));
    }

    public static OrderRestResponse from(AdminOrderDetailResult result) {
        return from(result.order(), result.customer());
    }
}
