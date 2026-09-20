package com.superfercho.orders.application.port;

import com.superfercho.orders.application.dto.PageRequest;
import com.superfercho.orders.application.dto.PagedResult;
import com.superfercho.orders.domain.model.Order;
import com.superfercho.orders.domain.model.OrderStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {

    Order save(Order order);

    /**
     * Persists a leaving-PENDING transition ({@code CONFIRMED} or {@code CANCELLED})
     * only if the stored row is still {@code PENDING}. Empty means another writer
     * already changed the status.
     */
    Optional<Order> saveIfPending(Order order);

    Optional<Order> findById(UUID orderId);

    PagedResult<Order> findByCustomerId(UUID customerId, PageRequest pageRequest);

    PagedResult<Order> findAll(PageRequest pageRequest);

    PagedResult<Order> findByStatuses(List<OrderStatus> statuses, PageRequest pageRequest);

    List<Order> findPendingOrdersEligibleForAutomaticConfirmation(Instant currentTime);
}
