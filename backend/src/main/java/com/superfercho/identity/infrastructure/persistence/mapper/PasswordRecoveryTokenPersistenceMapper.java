package com.superfercho.identity.infrastructure.persistence.mapper;

import com.superfercho.identity.domain.model.PasswordRecoveryToken;
import com.superfercho.identity.infrastructure.persistence.entity.PasswordRecoveryTokenJpaEntity;

public final class PasswordRecoveryTokenPersistenceMapper {

    private PasswordRecoveryTokenPersistenceMapper() {}

    public static PasswordRecoveryTokenJpaEntity toEntity(PasswordRecoveryToken token) {
        return new PasswordRecoveryTokenJpaEntity(
                token.id(),
                token.userId(),
                token.tokenHash(),
                token.requestIp(),
                token.createdAt(),
                token.expiresAt(),
                token.consumedAt());
    }

    public static PasswordRecoveryToken toDomain(PasswordRecoveryTokenJpaEntity entity) {
        return PasswordRecoveryToken.restore(
                entity.getId(),
                entity.getUserId(),
                entity.getTokenHash(),
                entity.getRequestIp(),
                entity.getCreatedAt(),
                entity.getExpiresAt(),
                entity.getConsumedAt());
    }
}
