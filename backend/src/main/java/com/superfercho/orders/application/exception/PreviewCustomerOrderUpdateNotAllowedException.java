package com.superfercho.orders.application.exception;

import java.util.UUID;

public class PreviewCustomerOrderUpdateNotAllowedException extends RuntimeException {

    public PreviewCustomerOrderUpdateNotAllowedException(UUID customerId) {
        super("Order status cannot be updated for storefront-preview customer: " + customerId);
    }
}
