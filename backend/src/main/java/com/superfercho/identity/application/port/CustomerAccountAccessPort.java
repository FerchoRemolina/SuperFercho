package com.superfercho.identity.application.port;

import java.util.UUID;

/**
 * Post-JWT check for commercial CUSTOMER accounts. Returns whether the user may continue with a
 * cryptographically valid CUSTOMER token ({@code deletedAt == null} and user present).
 */
public interface CustomerAccountAccessPort {

    boolean allowsCustomerAccess(UUID userId);
}
