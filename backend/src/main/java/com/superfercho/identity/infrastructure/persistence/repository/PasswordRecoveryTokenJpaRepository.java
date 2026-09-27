package com.superfercho.identity.infrastructure.persistence.repository;

import com.superfercho.identity.infrastructure.persistence.entity.PasswordRecoveryTokenJpaEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordRecoveryTokenJpaRepository
        extends JpaRepository<PasswordRecoveryTokenJpaEntity, UUID> {

    Optional<PasswordRecoveryTokenJpaEntity> findByTokenHash(String tokenHash);

    List<PasswordRecoveryTokenJpaEntity> findByUserIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
            UUID userId, Instant createdAt);

    void deleteAllByUserId(UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            """
            update PasswordRecoveryTokenJpaEntity t
               set t.consumedAt = :consumedAt
             where t.userId = :userId
               and t.consumedAt is null
            """)
    int consumeActiveForUser(@Param("userId") UUID userId, @Param("consumedAt") Instant consumedAt);
}
