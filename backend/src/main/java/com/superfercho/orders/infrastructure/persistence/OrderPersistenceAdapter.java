package com.superfercho.orders.infrastructure.persistence;

import com.superfercho.orders.application.dto.AdminCustomerSalesRow;
import com.superfercho.orders.application.dto.AdminOrderStatusCountRow;
import com.superfercho.orders.application.dto.AdminProductSalesRow;
import com.superfercho.orders.application.dto.AdminSalesBucketRow;
import com.superfercho.orders.application.dto.PageRequest;
import com.superfercho.orders.application.dto.PagedResult;
import com.superfercho.orders.application.port.OrderRepository;
import com.superfercho.orders.domain.model.Order;
import com.superfercho.orders.domain.model.OrderStatus;
import com.superfercho.orders.infrastructure.persistence.entity.OrderJpaEntity;
import com.superfercho.orders.infrastructure.persistence.mapper.OrderPersistenceMapper;
import com.superfercho.orders.infrastructure.persistence.repository.OrderJpaRepository;
import com.superfercho.platform.time.BucketGranularity;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class OrderPersistenceAdapter implements OrderRepository {

    private static final List<OrderStatus> IN_PROGRESS =
            List.of(OrderStatus.CONFIRMED, OrderStatus.PREPARING, OrderStatus.DELIVERY);

    private final OrderJpaRepository orderJpaRepository;
    private final OrderPersistenceMapper orderPersistenceMapper;

    public OrderPersistenceAdapter(
            OrderJpaRepository orderJpaRepository, OrderPersistenceMapper orderPersistenceMapper) {
        this.orderJpaRepository = orderJpaRepository;
        this.orderPersistenceMapper = orderPersistenceMapper;
    }

    @Override
    public Order save(Order order) {
        return orderPersistenceMapper.toDomain(
                orderJpaRepository.saveAndFlush(orderPersistenceMapper.toEntity(order)));
    }

    @Override
    public Optional<Order> saveIfConfirmed(Order order) {
        if (order.status() == OrderStatus.CONFIRMED) {
            throw new IllegalArgumentException("saveIfConfirmed requires leaving CONFIRMED, got CONFIRMED");
        }
        int updated = orderJpaRepository.updateStatusIfConfirmed(
                order.id(),
                order.status(),
                order.confirmedAt(),
                order.cancelledAt(),
                order.updatedAt(),
                OrderStatus.CONFIRMED);
        if (updated == 0) {
            return Optional.empty();
        }
        return findById(order.id());
    }

    @Override
    public Optional<Order> findById(UUID orderId) {
        return orderJpaRepository.findById(orderId).map(orderPersistenceMapper::toDomain);
    }

    @Override
    public PagedResult<Order> findByCustomerId(UUID customerId, PageRequest pageRequest) {
        return toPagedResult(orderJpaRepository.findAllByCustomerId(customerId, toSpringPage(pageRequest)), pageRequest);
    }

    @Override
    public PagedResult<Order> findByCustomerIds(Collection<UUID> customerIds, PageRequest pageRequest) {
        if (customerIds == null || customerIds.isEmpty()) {
            return new PagedResult<>(List.of(), pageRequest.page(), pageRequest.size(), 0);
        }
        return toPagedResult(
                orderJpaRepository.findAllByCustomerIdIn(customerIds, toSpringPage(pageRequest)), pageRequest);
    }

    @Override
    public PagedResult<Order> findOrdersWithPaymentByCustomerIds(
            Collection<UUID> customerIds, PageRequest pageRequest) {
        if (customerIds == null || customerIds.isEmpty()) {
            return new PagedResult<>(List.of(), pageRequest.page(), pageRequest.size(), 0);
        }
        return toPagedResult(
                orderJpaRepository.findAllByCustomerIdInAndPaymentIdIsNotNull(
                        customerIds, toSpringPage(pageRequest)),
                pageRequest);
    }

    @Override
    public PagedResult<Order> findAll(PageRequest pageRequest) {
        return toPagedResult(orderJpaRepository.findAll(toSpringPage(pageRequest)), pageRequest);
    }

    @Override
    public PagedResult<Order> findByStatuses(List<OrderStatus> statuses, PageRequest pageRequest) {
        return toPagedResult(
                orderJpaRepository.findAllByStatusIn(statuses, toSpringPage(pageRequest)), pageRequest);
    }

    @Override
    public List<Order> findInProgressForLifecycle() {
        return orderJpaRepository.findByStatusInAndConfirmedAtIsNotNull(IN_PROGRESS).stream()
                .map(orderPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<Order> findCreatedBetweenExcludingStatus(
            Instant fromInclusive, Instant toExclusive, OrderStatus excludedStatus) {
        return orderJpaRepository
                .findByCreatedAtGreaterThanEqualAndCreatedAtLessThanAndStatusNotOrderByCreatedAtAsc(
                        fromInclusive, toExclusive, excludedStatus)
                .stream()
                .map(orderPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public void deleteAllByCustomerId(UUID customerId) {
        orderJpaRepository.deleteAllByCustomerId(customerId);
    }

    @Override
    public List<AdminSalesBucketRow> aggregateSalesBuckets(
            BucketGranularity granularity, Instant fromInclusive, Instant toExclusive) {
        return orderJpaRepository
                .aggregateSalesBuckets(truncUnit(granularity), fromInclusive, toExclusive)
                .stream()
                .map(row -> new AdminSalesBucketRow(
                        toInstant(row[0]), requireBigDecimal(row[1]), requireLong(row[2])))
                .toList();
    }

    @Override
    public List<AdminOrderStatusCountRow> countByStatusBetween(Instant fromInclusive, Instant toExclusive) {
        return orderJpaRepository.countByStatusBetween(fromInclusive, toExclusive).stream()
                .map(row -> new AdminOrderStatusCountRow(
                        OrderStatus.valueOf(String.valueOf(row[0])), requireLong(row[1]), requireBigDecimal(row[2])))
                .toList();
    }

    @Override
    public List<AdminProductSalesRow> findTopProductsByQuantity(
            Instant fromInclusive, Instant toExclusive, int limit, boolean ascending) {
        List<Object[]> rows = ascending
                ? orderJpaRepository.findTopProductsByQuantityAsc(fromInclusive, toExclusive, limit)
                : orderJpaRepository.findTopProductsByQuantityDesc(fromInclusive, toExclusive, limit);
        return rows.stream()
                .map(row -> new AdminProductSalesRow(
                        (UUID) row[0], String.valueOf(row[1]), requireLong(row[2])))
                .toList();
    }

    @Override
    public List<AdminCustomerSalesRow> findTopCustomersByTotal(
            Instant fromInclusive, Instant toExclusive, int limit) {
        return orderJpaRepository.findTopCustomersByTotal(fromInclusive, toExclusive, limit).stream()
                .map(row -> new AdminCustomerSalesRow(
                        (UUID) row[0], String.valueOf(row[1]), requireBigDecimal(row[2]), requireLong(row[3])))
                .toList();
    }

    private static String truncUnit(BucketGranularity granularity) {
        return switch (granularity) {
            case HOUR -> "hour";
            case DAY -> "day";
            case MONTH -> "month";
        };
    }

    private static Instant toInstant(Object raw) {
        if (raw instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toInstant();
        }
        if (raw instanceof Timestamp timestamp) {
            return timestamp.toInstant();
        }
        if (raw instanceof Instant instant) {
            return instant;
        }
        if (raw instanceof LocalDateTime localDateTime) {
            return localDateTime.atZone(com.superfercho.platform.time.BusinessZone.BOGOTA)
                    .toInstant();
        }
        throw new IllegalStateException("Unsupported timestamp type from aggregation: " + raw.getClass());
    }

    private static BigDecimal requireBigDecimal(Object raw) {
        if (raw instanceof BigDecimal amount) {
            return amount;
        }
        if (raw instanceof Number number) {
            return BigDecimal.valueOf(number.longValue());
        }
        throw new IllegalStateException("Unsupported amount type from aggregation: " + raw.getClass());
    }

    private static long requireLong(Object raw) {
        if (raw instanceof Number number) {
            return number.longValue();
        }
        throw new IllegalStateException("Unsupported count type from aggregation: " + raw.getClass());
    }

    private PagedResult<Order> toPagedResult(Page<OrderJpaEntity> page, PageRequest pageRequest) {
        return new PagedResult<>(
                page.getContent().stream().map(orderPersistenceMapper::toDomain).toList(),
                pageRequest.page(),
                pageRequest.size(),
                page.getTotalElements());
    }

    private static org.springframework.data.domain.PageRequest toSpringPage(PageRequest pageRequest) {
        return org.springframework.data.domain.PageRequest.of(
                pageRequest.page(),
                pageRequest.size(),
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by("id")));
    }
}
