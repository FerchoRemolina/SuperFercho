package com.superfercho.identity.infrastructure.persistence.mapper;

import com.superfercho.identity.domain.model.User;
import com.superfercho.identity.infrastructure.persistence.entity.UserJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class UserPersistenceMapper {

    public UserJpaEntity toEntity(User user) {
        return new UserJpaEntity(
                user.id(),
                user.documentType(),
                user.documentNumber(),
                user.fullName(),
                user.email(),
                user.phone(),
                user.passwordHash(),
                user.role(),
                user.status(),
                user.createdAt(),
                user.updatedAt());
    }

    public User toDomain(UserJpaEntity entity) {
        return User.create(
                entity.getId(),
                entity.getDocumentType(),
                entity.getDocumentNumber(),
                entity.getFullName(),
                entity.getEmail(),
                entity.getPhone(),
                entity.getPasswordHash(),
                entity.getRole(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
