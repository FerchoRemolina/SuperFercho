package com.superfercho.identity.infrastructure.persistence;

import com.superfercho.identity.application.port.PasswordRecoveryTokenRepository;
import com.superfercho.identity.domain.model.PasswordRecoveryToken;
import com.superfercho.identity.infrastructure.persistence.mapper.PasswordRecoveryTokenPersistenceMapper;
import com.superfercho.identity.infrastructure.persistence.repository.PasswordRecoveryTokenJpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("!test")
public class PasswordRecoveryTokenPersistenceAdapter implements PasswordRecoveryTokenRepository {

    private final PasswordRecoveryTokenJpaRepository repository;

    public PasswordRecoveryTokenPersistenceAdapter(PasswordRecoveryTokenJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public PasswordRecoveryToken save(PasswordRecoveryToken token) {
        return PasswordRecoveryTokenPersistenceMapper.toDomain(
                repository.saveAndFlush(PasswordRecoveryTokenPersistenceMapper.toEntity(token)));
    }

    @Override
    public Optional<PasswordRecoveryToken> findByTokenHash(String tokenHash) {
        return repository.findByTokenHash(tokenHash).map(PasswordRecoveryTokenPersistenceMapper::toDomain);
    }

    @Override
    public List<PasswordRecoveryToken> findByUserIdCreatedAtOrAfter(UUID userId, Instant createdAtOrAfter) {
        return repository
                .findByUserIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(userId, createdAtOrAfter)
                .stream()
                .map(PasswordRecoveryTokenPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void deleteAllByUserId(UUID userId) {
        repository.deleteAllByUserId(userId);
    }

    @Override
    @Transactional
    public void consumeActiveTokensForUser(UUID userId, Instant consumedAt) {
        repository.consumeActiveForUser(userId, consumedAt);
    }
}
