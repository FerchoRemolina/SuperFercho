package com.superfercho.identity.infrastructure.persistence.repository;

import com.superfercho.identity.infrastructure.persistence.entity.UserJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {

    boolean existsByEmail(String email);

    boolean existsByDocumentTypeAndDocumentNumber(String documentType, String documentNumber);

    Optional<UserJpaEntity> findByEmail(String email);
}
