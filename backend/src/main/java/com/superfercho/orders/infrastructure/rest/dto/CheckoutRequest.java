package com.superfercho.orders.infrastructure.rest.dto;

import com.superfercho.orders.application.dto.PaymentMethod;
import java.util.List;
import java.util.UUID;

public record CheckoutRequest(UUID addressId, PaymentMethod paymentMethod, List<CheckoutItemRequest> items) {
}
