package com.superfercho.orders.infrastructure.persistence.repository;

import com.superfercho.orders.domain.model.OrderStatus;
import com.superfercho.orders.infrastructure.persistence.entity.OrderJpaEntity;
import java.time.Instant;
import java.util.Collection;
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

    Page<OrderJpaEntity> findAllByCustomerIdIn(Collection<UUID> customerIds, Pageable pageable);

    Page<OrderJpaEntity> findAllByCustomerIdInAndPaymentIdIsNotNull(
            Collection<UUID> customerIds, Pageable pageable);

    Page<OrderJpaEntity> findAllByStatusIn(List<OrderStatus> statuses, Pageable pageable);

    Page<OrderJpaEntity> findAllByOrderNumberContainsIgnoreCase(String orderNumber, Pageable pageable);

    Page<OrderJpaEntity> findAllByStatusInAndOrderNumberContainsIgnoreCase(
            List<OrderStatus> statuses, String orderNumber, Pageable pageable);

    Page<OrderJpaEntity> findAllByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            Instant fromInclusive, Instant toExclusive, Pageable pageable);

    Page<OrderJpaEntity> findAllByStatusInAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            List<OrderStatus> statuses, Instant fromInclusive, Instant toExclusive, Pageable pageable);

    Page<OrderJpaEntity> findAllByOrderNumberContainsIgnoreCaseAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            String orderNumber, Instant fromInclusive, Instant toExclusive, Pageable pageable);

    Page<OrderJpaEntity>
            findAllByStatusInAndOrderNumberContainsIgnoreCaseAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                    List<OrderStatus> statuses, String orderNumber, Instant fromInclusive, Instant toExclusive,
                    Pageable pageable);

    Page<OrderJpaEntity> findAllByCreatedAtGreaterThanEqual(Instant fromInclusive, Pageable pageable);

    Page<OrderJpaEntity> findAllByCreatedAtLessThan(Instant toExclusive, Pageable pageable);

    Page<OrderJpaEntity> findAllByStatusInAndCreatedAtGreaterThanEqual(
            List<OrderStatus> statuses, Instant fromInclusive, Pageable pageable);

    Page<OrderJpaEntity> findAllByStatusInAndCreatedAtLessThan(
            List<OrderStatus> statuses, Instant toExclusive, Pageable pageable);

    Page<OrderJpaEntity> findAllByOrderNumberContainsIgnoreCaseAndCreatedAtGreaterThanEqual(
            String orderNumber, Instant fromInclusive, Pageable pageable);

    Page<OrderJpaEntity> findAllByOrderNumberContainsIgnoreCaseAndCreatedAtLessThan(
            String orderNumber, Instant toExclusive, Pageable pageable);

    Page<OrderJpaEntity> findAllByStatusInAndOrderNumberContainsIgnoreCaseAndCreatedAtGreaterThanEqual(
            List<OrderStatus> statuses, String orderNumber, Instant fromInclusive, Pageable pageable);

    Page<OrderJpaEntity> findAllByStatusInAndOrderNumberContainsIgnoreCaseAndCreatedAtLessThan(
            List<OrderStatus> statuses, String orderNumber, Instant toExclusive, Pageable pageable);

    List<OrderJpaEntity> findByStatusInAndConfirmedAtIsNotNull(List<OrderStatus> statuses);

    List<OrderJpaEntity> findByCreatedAtGreaterThanEqualAndCreatedAtLessThanAndStatusNotOrderByCreatedAtAsc(
            Instant fromInclusive, Instant toExclusive, OrderStatus excludedStatus);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(
            """
            update OrderJpaEntity o
               set o.status = :newStatus,
                   o.confirmedAt = :confirmedAt,
                   o.cancelledAt = :cancelledAt,
                   o.deliveredAt = :deliveredAt,
                   o.updatedAt = :updatedAt
             where o.id = :id
               and o.status = :fromStatus
            """)
    int updateStatusIfCurrent(
            @Param("id") UUID id,
            @Param("fromStatus") OrderStatus fromStatus,
            @Param("newStatus") OrderStatus newStatus,
            @Param("confirmedAt") Instant confirmedAt,
            @Param("cancelledAt") Instant cancelledAt,
            @Param("deliveredAt") Instant deliveredAt,
            @Param("updatedAt") Instant updatedAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    void deleteAllByCustomerId(UUID customerId);

    @Query(
            value =
                    """
                    SELECT date_trunc(CAST(:unit AS text), o.delivered_at AT TIME ZONE 'America/Bogota')
                               AT TIME ZONE 'America/Bogota' AS bucket_start,
                           COALESCE(SUM(o.total_amount), 0) AS total_amount,
                           COUNT(*) AS order_count
                      FROM orders.orders o
                     WHERE o.status = 'DELIVERED'
                       AND o.delivered_at >= :fromInclusive
                       AND o.delivered_at < :toExclusive
                     GROUP BY 1
                     ORDER BY 1
                    """,
            nativeQuery = true)
    List<Object[]> aggregateSalesBuckets(
            @Param("unit") String unit,
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive);

    @Query(
            value =
                    """
                    SELECT
                        COUNT(*) FILTER (
                            WHERE o.status = 'DELIVERED'
                              AND o.delivered_at >= :fromInclusive
                              AND o.delivered_at < :toExclusive),
                        COUNT(*) FILTER (
                            WHERE o.status = 'CANCELLED'
                              AND o.cancelled_at >= :fromInclusive
                              AND o.cancelled_at < :toExclusive),
                        COALESCE(SUM(o.total_amount) FILTER (
                            WHERE o.status = 'DELIVERED'
                              AND o.delivered_at >= :fromInclusive
                              AND o.delivered_at < :toExclusive), 0),
                        COUNT(*) FILTER (
                            WHERE o.status IN ('CONFIRMED', 'PREPARING', 'DELIVERY'))
                      FROM orders.orders o
                    """,
            nativeQuery = true)
    List<Object[]> summarizeBusinessPeriod(
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive);

    List<OrderJpaEntity> findAllByStatusAndDeliveredAtGreaterThanEqualAndDeliveredAtLessThan(
            OrderStatus status, Instant fromInclusive, Instant toExclusive);

    @Query(
            value =
                    """
                    SELECT oi.product_id AS product_id,
                           MAX(oi.product_name) AS product_name,
                           SUM(oi.quantity) AS quantity
                      FROM orders.order_items oi
                      JOIN orders.orders o ON o.id = oi.order_id
                     WHERE o.created_at >= :fromInclusive
                       AND o.created_at < :toExclusive
                       AND o.status <> 'CANCELLED'
                     GROUP BY oi.product_id
                     ORDER BY quantity DESC, oi.product_id ASC
                     LIMIT :limit
                    """,
            nativeQuery = true)
    List<Object[]> findTopProductsByQuantityDesc(
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive,
            @Param("limit") int limit);

    @Query(
            value =
                    """
                    SELECT oi.product_id AS product_id,
                           MAX(oi.product_name) AS product_name,
                           SUM(oi.quantity) AS quantity
                      FROM orders.order_items oi
                      JOIN orders.orders o ON o.id = oi.order_id
                     WHERE o.created_at >= :fromInclusive
                       AND o.created_at < :toExclusive
                       AND o.status <> 'CANCELLED'
                     GROUP BY oi.product_id
                     ORDER BY quantity ASC, oi.product_id ASC
                     LIMIT :limit
                    """,
            nativeQuery = true)
    List<Object[]> findTopProductsByQuantityAsc(
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive,
            @Param("limit") int limit);

    @Query(
            value =
                    """
                    SELECT o.customer_id AS customer_id,
                           MAX(o.shipping_recipient_name) AS customer_name,
                           COALESCE(SUM(o.total_amount), 0) AS total_amount,
                           COUNT(*) AS order_count
                      FROM orders.orders o
                     WHERE o.created_at >= :fromInclusive
                       AND o.created_at < :toExclusive
                       AND o.status <> 'CANCELLED'
                     GROUP BY o.customer_id
                     ORDER BY order_count DESC, total_amount DESC, o.customer_id ASC
                     LIMIT :limit
                    """,
            nativeQuery = true)
    List<Object[]> findTopCustomersByOrderCount(
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive,
            @Param("limit") int limit);

    @Query(
            value =
                    """
                    SELECT o.customer_id AS customer_id,
                           MAX(o.shipping_recipient_name) AS customer_name,
                           COALESCE(SUM(o.total_amount), 0) AS total_amount,
                           COUNT(*) AS order_count
                      FROM orders.orders o
                     WHERE o.created_at >= :fromInclusive
                       AND o.created_at < :toExclusive
                       AND o.status <> 'CANCELLED'
                     GROUP BY o.customer_id
                     ORDER BY total_amount DESC, o.customer_id ASC
                     LIMIT :limit
                    """,
            nativeQuery = true)
    List<Object[]> findTopCustomersByTotal(
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive,
            @Param("limit") int limit);
}
