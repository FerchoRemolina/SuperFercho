package com.superfercho.orders.application.port;

import com.superfercho.orders.application.dto.PageRequest;
import com.superfercho.orders.application.dto.PagedResult;
import com.superfercho.orders.domain.model.Order;
import com.superfercho.orders.domain.model.OrderStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {

    Order save(Order order);

    /**
     * Persists a leaving-CONFIRMED transition ({@code PREPARING} or {@code CANCELLED})
     * only if the stored row is still {@code CONFIRMED}. Empty means another writer
     * already changed the status (cancel vs lifecycle race).
     */
    Optional<Order> saveIfConfirmed(Order order);

    Optional<Order> findById(UUID orderId);

    PagedResult<Order> findByCustomerId(UUID customerId, PageRequest pageRequest);

    PagedResult<Order> findByCustomerIds(Collection<UUID> customerIds, PageRequest pageRequest);

    /**
     * Orders that already have a payment id, for the given operational customer ids (newest first).
     */
    PagedResult<Order> findOrdersWithPaymentByCustomerIds(
            Collection<UUID> customerIds, PageRequest pageRequest);

    PagedResult<Order> findAll(PageRequest pageRequest);

    PagedResult<Order> findByStatuses(List<OrderStatus> statuses, PageRequest pageRequest);

    /** In-progress orders eligible for automatic lifecycle progression. */
    List<Order> findInProgressForLifecycle();

    void deleteAllByCustomerId(UUID customerId);
}
