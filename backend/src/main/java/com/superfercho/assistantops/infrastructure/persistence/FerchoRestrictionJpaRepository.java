package com.superfercho.assistantops.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FerchoRestrictionJpaRepository extends JpaRepository<FerchoRestrictionEntity, UUID> {

    Optional<FerchoRestrictionEntity> findFirstByUserIdAndLiftedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
            UUID userId, Instant now);

    boolean existsByUserIdAndTypeAndCreatedAtAfterAndIdNot(
            UUID userId, String type, Instant after, UUID excludedId);

    List<FerchoRestrictionEntity> findByLiftedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(Instant now);

    List<FerchoRestrictionEntity> findTop12ByUserIdOrderByCreatedAtDesc(UUID userId);

    List<FerchoRestrictionEntity> findByLiftedAtIsNullAndExpiresAtAfter(Instant now);

    long countByTypeAndLiftedAtIsNullAndExpiresAtAfter(String type, Instant now);
}
