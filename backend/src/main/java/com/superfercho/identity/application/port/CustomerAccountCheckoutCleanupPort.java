package com.superfercho.identity.application.port;

import java.util.UUID;

/**
 * Deletes checkout operational data (idempotency keys) for a customer. Must never delete orders or
 * payments.
 */
public interface CustomerAccountCheckoutCleanupPort {

    void deleteCheckoutIdempotencyForCustomer(UUID customerId);
}
