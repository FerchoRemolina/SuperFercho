package com.superfercho.identity.application.port;

import java.util.UUID;

/** Deletes shopping operational data owned exclusively by a customer account. */
public interface CustomerAccountShoppingCleanupPort {

    void deleteAllForCustomer(UUID customerId);
}
