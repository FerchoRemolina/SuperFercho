package com.superfercho.identity.application.fakes;

import com.superfercho.identity.application.port.PasswordRecoveryTokenRepository;
import com.superfercho.identity.domain.model.PasswordRecoveryToken;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryPasswordRecoveryTokenRepository implements PasswordRecoveryTokenRepository {

    private final Map<UUID, PasswordRecoveryToken> byId = new ConcurrentHashMap<>();

    @Override
    public PasswordRecoveryToken save(PasswordRecoveryToken token) {
        byId.put(token.id(), token);
        return token;
    }

    @Override
    public Optional<PasswordRecoveryToken> findByTokenHash(String tokenHash) {
        return byId.values().stream().filter(token -> token.tokenHash().equals(tokenHash)).findFirst();
    }

    @Override
    public List<PasswordRecoveryToken> findByUserIdCreatedAtOrAfter(UUID userId, Instant createdAtOrAfter) {
        List<PasswordRecoveryToken> matches = new ArrayList<>();
        for (PasswordRecoveryToken token : byId.values()) {
            if (token.userId().equals(userId)
                    && !token.createdAt().isBefore(createdAtOrAfter)) {
                matches.add(token);
            }
        }
        matches.sort((a, b) -> b.createdAt().compareTo(a.createdAt()));
        return List.copyOf(matches);
    }

    @Override
    public void deleteAllByUserId(UUID userId) {
        byId.values().removeIf(token -> token.userId().equals(userId));
    }

    @Override
    public void consumeActiveTokensForUser(UUID userId, Instant consumedAt) {
        for (PasswordRecoveryToken token : List.copyOf(byId.values())) {
            if (token.userId().equals(userId) && !token.isConsumed()) {
                byId.put(token.id(), token.consume(consumedAt));
            }
        }
    }

    public List<PasswordRecoveryToken> all() {
        return List.copyOf(byId.values());
    }
}
