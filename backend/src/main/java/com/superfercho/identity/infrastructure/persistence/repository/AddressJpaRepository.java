package com.superfercho.identity.infrastructure.persistence.repository;

import com.superfercho.identity.domain.model.AddressStatus;
import com.superfercho.identity.infrastructure.persistence.entity.AddressJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface AddressJpaRepository extends JpaRepository<AddressJpaEntity, UUID> {

    @Query(
            """
            select a from AddressJpaEntity a
            where a.userId = :userId and a.isDefault = true and a.status = :status
            """)
    Optional<AddressJpaEntity> findByUserIdAndIsDefaultTrueAndStatus(
            @Param("userId") UUID userId, @Param("status") AddressStatus status);

    List<AddressJpaEntity> findByUserId(UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    void deleteByUserId(UUID userId);
}
