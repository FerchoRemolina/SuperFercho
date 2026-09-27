package com.superfercho.identity.application.port;

import com.superfercho.identity.domain.model.PasswordRecoveryToken;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PasswordRecoveryTokenRepository {

    PasswordRecoveryToken save(PasswordRecoveryToken token);

    Optional<PasswordRecoveryToken> findByTokenHash(String tokenHash);

    List<PasswordRecoveryToken> findByUserIdCreatedAtOrAfter(UUID userId, Instant createdAtOrAfter);

    void deleteAllByUserId(UUID userId);

    void consumeActiveTokensForUser(UUID userId, Instant consumedAt);
}
