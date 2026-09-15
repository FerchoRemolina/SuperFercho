package com.superfercho.orders.infrastructure.rest.dto;

import com.superfercho.orders.application.dto.OrderItemResult;
import com.superfercho.platform.money.Money;
import java.util.UUID;

public record OrderItemRestResponse(
        UUID id, UUID productId, String productName, Money unitPrice, int quantity, Money subtotal) {

    public static OrderItemRestResponse from(OrderItemResult item) {
        return new OrderItemRestResponse(
                item.id(), item.productId(), item.productName(), item.unitPrice(), item.quantity(), item.subtotal());
    }
}
