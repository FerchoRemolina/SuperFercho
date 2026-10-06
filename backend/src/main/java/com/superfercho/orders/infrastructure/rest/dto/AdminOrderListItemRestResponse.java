package com.superfercho.orders.infrastructure.rest.dto;

import com.superfercho.orders.application.dto.AdminOrderListItemResult;
import com.superfercho.orders.domain.model.OrderStatus;
import com.superfercho.platform.money.Money;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Admin list row. Lean on purpose: no shipping snapshot beyond the recipient
 * name and no payment object (paymentId only). Items are included to keep
 * payload parity with the previous full-order rows (used by the Admin Hub
 * "recently sold" widget). Customer is null when the commercial identity
 * could not be resolved.
 */
public record AdminOrderListItemRestResponse(
        UUID id,
        String orderNumber,
        UUID customerId,
        AdminOrderCustomerRestResponse customer,
        OrderStatus status,
        Money subtotal,
        Money total,
        List<OrderItemRestResponse> items,
        String shippingRecipientName,
        UUID paymentId,
        Instant createdAt,
        Instant confirmedAt,
        Instant cancelledAt,
        Instant updatedAt) {

    public static AdminOrderListItemRestResponse from(AdminOrderListItemResult result) {
        return new AdminOrderListItemRestResponse(
                result.order().id(),
                result.order().orderNumber(),
                result.order().customerId(),
                result.customer() == null ? null : AdminOrderCustomerRestResponse.from(result.customer()),
                result.order().status(),
                result.order().subtotal(),
                result.order().total(),
                result.order().items().stream().map(OrderItemRestResponse::from).toList(),
                result.order().shippingAddress().recipientName(),
                result.order().paymentId(),
                result.order().createdAt(),
                result.order().confirmedAt(),
                result.order().cancelledAt(),
                result.order().updatedAt());
    }
}
