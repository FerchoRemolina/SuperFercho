package com.superfercho.orders.infrastructure.persistence.repository;

import com.superfercho.orders.infrastructure.persistence.entity.CheckoutIdempotencyJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckoutIdempotencyJpaRepository extends JpaRepository<CheckoutIdempotencyJpaEntity, UUID> {

    Optional<CheckoutIdempotencyJpaEntity> findByCustomerIdAndIdempotencyKey(UUID customerId, String idempotencyKey);
}
