package com.superfercho.orders.application.port;

import java.util.UUID;

/**
 * Allows Orders fulfillment flows to skip operational transitions for storefront-preview
 * temporary customers without embedding Identity types in Orders.
 */
public interface PreviewCustomerExclusionPort {

    boolean isPreviewTemporaryCustomer(UUID customerId);
}
