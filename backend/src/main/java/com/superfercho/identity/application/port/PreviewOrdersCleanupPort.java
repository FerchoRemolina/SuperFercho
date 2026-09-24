package com.superfercho.identity.application.port;

import java.util.UUID;

public interface PreviewOrdersCleanupPort {

    void cancelAndDeleteAllForCustomer(UUID customerId);
}
