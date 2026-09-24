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
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, UUID> {

    Optional<OrderJpaEntity> findByOrderNumber(String orderNumber);

    Page<OrderJpaEntity> findAllByCustomerId(UUID customerId, Pageable pageable);

    Page<OrderJpaEntity> findAllByStatusIn(List<OrderStatus> statuses, Pageable pageable);

    List<OrderJpaEntity> findByStatusAndCreatedAtBefore(OrderStatus status, Instant createdAtBefore);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(
            """
            update OrderJpaEntity o
               set o.status = :newStatus,
                   o.confirmedAt = :confirmedAt,
                   o.cancelledAt = :cancelledAt,
                   o.updatedAt = :updatedAt
             where o.id = :id
               and o.status = :pendingStatus
            """)
    int updateStatusIfPending(
            @Param("id") UUID id,
            @Param("newStatus") OrderStatus newStatus,
            @Param("confirmedAt") Instant confirmedAt,
            @Param("cancelledAt") Instant cancelledAt,
            @Param("updatedAt") Instant updatedAt,
            @Param("pendingStatus") OrderStatus pendingStatus);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    void deleteAllByCustomerId(UUID customerId);
}
