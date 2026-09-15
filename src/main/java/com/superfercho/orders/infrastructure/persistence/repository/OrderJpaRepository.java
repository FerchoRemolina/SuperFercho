package com.superfercho.orders.infrastructure.persistence.repository;

import com.superfercho.orders.domain.model.OrderStatus;
import com.superfercho.orders.infrastructure.persistence.entity.OrderJpaEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, UUID> {

    Optional<OrderJpaEntity> findByOrderNumber(String orderNumber);

    Page<OrderJpaEntity> findAllByCustomerId(UUID customerId, Pageable pageable);

    List<OrderJpaEntity> findByStatusAndCreatedAtBefore(OrderStatus status, Instant createdAtBefore);
}
