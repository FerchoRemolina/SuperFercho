package com.superfercho.identity.application.port;

import java.util.UUID;

/** Deletes assistant operational state owned exclusively by a user account. */
public interface CustomerAccountAssistantCleanupPort {

    void deleteAllForUser(UUID userId);
}
