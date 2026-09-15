package com.superfercho.orders.infrastructure.rest.dto;

import com.superfercho.orders.application.dto.CheckoutResult;
import com.superfercho.orders.application.dto.PaymentStatus;
import com.superfercho.orders.domain.model.OrderStatus;
import com.superfercho.platform.money.Money;
import java.util.UUID;

public record CheckoutRestResponse(
        UUID orderId, String orderNumber, OrderStatus status, PaymentStatus paymentStatus, Money total) {

    public static CheckoutRestResponse from(CheckoutResult result) {
        return new CheckoutRestResponse(
                result.orderId(), result.orderNumber(), result.status(), result.paymentStatus(), result.total());
    }
}
