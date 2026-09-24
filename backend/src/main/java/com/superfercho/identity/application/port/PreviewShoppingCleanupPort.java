package com.superfercho.identity.application.port;

import java.util.UUID;

public interface PreviewShoppingCleanupPort {

    void deleteAllForCustomer(UUID customerId);
}
