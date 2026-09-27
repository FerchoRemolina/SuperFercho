package com.superfercho.orders.application.port;

import java.util.UUID;

/**
 * Identifies storefront-preview temporary customers so Orders can skip persistent inventory
 * mutations (decrement on checkout, restore on cancel) without embedding Identity types.
 */
public interface PreviewCustomerExclusionPort {

    boolean isPreviewTemporaryCustomer(UUID customerId);
}
