package com.superfercho.shopping.infrastructure.persistence.repository;

import com.superfercho.shopping.infrastructure.persistence.entity.CartAddOperationJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

public interface CartAddOperationJpaRepository extends JpaRepository<CartAddOperationJpaEntity, UUID> {

    Optional<CartAddOperationJpaEntity> findByOperationId(UUID operationId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    void deleteAllByCustomerId(UUID customerId);
}
