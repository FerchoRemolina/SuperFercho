package com.superfercho.identity.infrastructure.persistence.repository;

import com.superfercho.identity.infrastructure.persistence.entity.CustomerRecordJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRecordJpaRepository extends JpaRepository<CustomerRecordJpaEntity, UUID> {

    boolean existsByDocumentTypeAndDocumentNumber(String documentType, String documentNumber);

    Optional<CustomerRecordJpaEntity> findByDocumentTypeAndDocumentNumber(
            String documentType, String documentNumber);
}
