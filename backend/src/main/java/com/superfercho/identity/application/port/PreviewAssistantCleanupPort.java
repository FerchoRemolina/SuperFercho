package com.superfercho.identity.application.port;

import java.util.UUID;

public interface PreviewAssistantCleanupPort {

    void deleteAllForUser(UUID userId);
}
