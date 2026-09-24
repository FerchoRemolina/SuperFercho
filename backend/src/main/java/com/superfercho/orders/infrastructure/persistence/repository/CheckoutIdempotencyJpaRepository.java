package com.superfercho.orders.infrastructure.persistence.repository;

import com.superfercho.orders.infrastructure.persistence.entity.CheckoutIdempotencyJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

public interface CheckoutIdempotencyJpaRepository extends JpaRepository<CheckoutIdempotencyJpaEntity, UUID> {

    Optional<CheckoutIdempotencyJpaEntity> findByCustomerIdAndIdempotencyKey(UUID customerId, String idempotencyKey);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    void deleteAllByCustomerId(UUID customerId);
}
