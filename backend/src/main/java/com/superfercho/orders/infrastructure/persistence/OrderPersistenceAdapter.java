package com.superfercho.orders.infrastructure.persistence;

import com.superfercho.orders.application.dto.PageRequest;
import com.superfercho.orders.application.dto.PagedResult;
import com.superfercho.orders.application.port.OrderRepository;
import com.superfercho.orders.domain.model.Order;
import com.superfercho.orders.domain.model.OrderStatus;
import com.superfercho.orders.infrastructure.persistence.entity.OrderJpaEntity;
import com.superfercho.orders.infrastructure.persistence.mapper.OrderPersistenceMapper;
import com.superfercho.orders.infrastructure.persistence.repository.OrderJpaRepository;
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
    public void deleteAllByCustomerId(UUID customerId) {
        orderJpaRepository.deleteAllByCustomerId(customerId);
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
