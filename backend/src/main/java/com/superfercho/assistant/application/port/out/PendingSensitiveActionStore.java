package com.superfercho.assistant.application.port.out;

import com.superfercho.assistant.application.confirmation.PendingSensitiveAction;
import java.util.Optional;
import java.util.UUID;

public interface PendingSensitiveActionStore {

    void save(PendingSensitiveAction action);

    Optional<PendingSensitiveAction> findByToken(String token);

    void delete(String token);

    void deleteByUserId(UUID userId);
}
