package com.superfercho.shopping.infrastructure.persistence.repository;

import com.superfercho.shopping.infrastructure.persistence.entity.CartJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

public interface CartJpaRepository extends JpaRepository<CartJpaEntity, UUID> {

    Optional<CartJpaEntity> findByCustomerId(UUID customerId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    void deleteByCustomerId(UUID customerId);
}
