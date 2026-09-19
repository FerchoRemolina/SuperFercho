package com.superfercho.assistant.infrastructure.confirmation;

import com.superfercho.assistant.application.confirmation.PendingSensitiveAction;
import com.superfercho.assistant.application.port.out.PendingSensitiveActionStore;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryPendingSensitiveActionStore implements PendingSensitiveActionStore {

    private final Map<String, PendingSensitiveAction> byToken = new ConcurrentHashMap<>();

    @Override
    public void save(PendingSensitiveAction action) {
        byToken.put(action.token(), action);
    }

    @Override
    public Optional<PendingSensitiveAction> findByToken(String token) {
        return Optional.ofNullable(byToken.get(token));
    }

    @Override
    public void delete(String token) {
        byToken.remove(token);
    }

    @Override
    public void deleteByUserId(UUID userId) {
        byToken.values().removeIf(action -> action.userId().equals(userId));
    }
}
