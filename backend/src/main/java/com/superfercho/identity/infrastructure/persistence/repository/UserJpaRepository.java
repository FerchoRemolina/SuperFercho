package com.superfercho.identity.infrastructure.persistence.repository;

import com.superfercho.identity.infrastructure.persistence.entity.UserJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {

    boolean existsByEmailAndDeletedAtIsNull(String email);

    boolean existsByDocumentTypeAndDocumentNumber(String documentType, String documentNumber);

    Optional<UserJpaEntity> findByEmailAndDeletedAtIsNull(String email);

    Optional<UserJpaEntity> findByDocumentTypeAndDocumentNumber(String documentType, String documentNumber);

    Optional<UserJpaEntity> findByCustomerRecordIdAndDeletedAtIsNull(UUID customerRecordId);
}
