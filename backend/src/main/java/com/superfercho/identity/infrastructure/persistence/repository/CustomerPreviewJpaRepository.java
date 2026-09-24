package com.superfercho.identity.infrastructure.persistence.repository;

import com.superfercho.identity.domain.model.CustomerPreviewStatus;
import com.superfercho.identity.infrastructure.persistence.entity.CustomerPreviewJpaEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerPreviewJpaRepository extends JpaRepository<CustomerPreviewJpaEntity, UUID> {

    Optional<CustomerPreviewJpaEntity> findByAdminUserIdAndStatus(
            UUID adminUserId, CustomerPreviewStatus status);

    Optional<CustomerPreviewJpaEntity> findByTemporaryCustomerId(UUID temporaryCustomerId);

    boolean existsByTemporaryCustomerId(UUID temporaryCustomerId);

    @Query(
            """
            select p from CustomerPreviewJpaEntity p
            where p.status = com.superfercho.identity.domain.model.CustomerPreviewStatus.ACTIVE
              and p.expiresAt <= :now
            """)
    List<CustomerPreviewJpaEntity> findExpiredActive(@Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            """
            update CustomerPreviewJpaEntity p
               set p.status = com.superfercho.identity.domain.model.CustomerPreviewStatus.CLOSED,
                   p.closedAt = :closedAt
             where p.id = :id
               and p.status = com.superfercho.identity.domain.model.CustomerPreviewStatus.ACTIVE
            """)
    int claimClose(@Param("id") UUID id, @Param("closedAt") Instant closedAt);
}
