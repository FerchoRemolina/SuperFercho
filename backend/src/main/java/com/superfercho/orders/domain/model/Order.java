package com.superfercho.orders.domain.model;

import com.superfercho.orders.domain.exception.InvalidOrderException;
import com.superfercho.orders.domain.exception.InvalidOrderStateTransitionException;
import com.superfercho.orders.domain.exception.OrderCancellationNotAllowedException;
import com.superfercho.platform.money.Money;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class Order {

    public static final Duration CUSTOMER_CANCELLATION_WINDOW = Duration.ofMinutes(2);
    public static final Duration PREPARING_AFTER = Duration.ofMinutes(2);
    public static final Duration DELIVERY_AFTER = Duration.ofMinutes(4);
    public static final Duration DELIVERED_AFTER = Duration.ofMinutes(6);

    private final UUID id;
    private final OrderNumber orderNumber;
    private final UUID customerId;
    private final OrderStatus status;
    private final List<OrderItem> items;
    private final Money subtotal;
    private final Money total;
    private final ShippingAddressSnapshot shippingAddress;
    private final UUID paymentId;
    private final Instant createdAt;
    private final Instant confirmedAt;
    private final Instant cancelledAt;
    private final Instant updatedAt;

    private Order(
            UUID id,
            OrderNumber orderNumber,
            UUID customerId,
            OrderStatus status,
            List<OrderItem> items,
            Money subtotal,
            Money total,
            ShippingAddressSnapshot shippingAddress,
            UUID paymentId,
            Instant createdAt,
            Instant confirmedAt,
            Instant cancelledAt,
            Instant updatedAt) {
        this.id = id;
        this.orderNumber = orderNumber;
        this.customerId = customerId;
        this.status = status;
        this.items = items;
        this.subtotal = subtotal;
        this.total = total;
        this.shippingAddress = shippingAddress;
        this.paymentId = paymentId;
        this.createdAt = createdAt;
        this.confirmedAt = confirmedAt;
        this.cancelledAt = cancelledAt;
        this.updatedAt = updatedAt;
    }

    public static Order create(
            UUID id,
            OrderNumber orderNumber,
            UUID customerId,
            List<OrderItem> items,
            ShippingAddressSnapshot shippingAddress,
            UUID paymentId,
            Instant createdAt,
            Instant updatedAt) {
        return of(
                id,
                orderNumber,
                customerId,
                OrderStatus.CONFIRMED,
                items,
                shippingAddress,
                paymentId,
                createdAt,
                createdAt,
                null,
                updatedAt);
    }

    public static Order reconstitute(
            UUID id,
            OrderNumber orderNumber,
            UUID customerId,
            OrderStatus status,
            List<OrderItem> items,
            ShippingAddressSnapshot shippingAddress,
            UUID paymentId,
            Instant createdAt,
            Instant confirmedAt,
            Instant cancelledAt,
            Instant updatedAt) {
        return of(
                id,
                orderNumber,
                customerId,
                status,
                items,
                shippingAddress,
                paymentId,
                createdAt,
                confirmedAt,
                cancelledAt,
                updatedAt);
    }

    /**
     * Lifecycle status expected from {@code confirmedAt} at {@code currentTime}.
     * Used by the automatic progression job (including catch-up).
     */
    public static OrderStatus targetStatusAt(Instant confirmedAt, Instant currentTime) {
        requireStaticNonNull(confirmedAt, "confirmedAt");
        requireStaticNonNull(currentTime, "currentTime");
        if (currentTime.isBefore(confirmedAt)) {
            throw new InvalidOrderException("currentTime must not be before confirmedAt");
        }
        Duration elapsed = Duration.between(confirmedAt, currentTime);
        if (elapsed.compareTo(PREPARING_AFTER) < 0) {
            return OrderStatus.CONFIRMED;
        }
        if (elapsed.compareTo(DELIVERY_AFTER) < 0) {
            return OrderStatus.PREPARING;
        }
        if (elapsed.compareTo(DELIVERED_AFTER) < 0) {
            return OrderStatus.DELIVERY;
        }
        return OrderStatus.DELIVERED;
    }

    public Order startPreparation(Instant currentTime) {
        Instant at = requireCurrentTime(currentTime);
        requireTransition(OrderStatus.PREPARING);
        return withStatus(OrderStatus.PREPARING, at);
    }

    public Order startDelivery(Instant currentTime) {
        Instant at = requireCurrentTime(currentTime);
        requireTransition(OrderStatus.DELIVERY);
        return withStatus(OrderStatus.DELIVERY, at);
    }

    public Order markDelivered(Instant currentTime) {
        Instant at = requireCurrentTime(currentTime);
        requireTransition(OrderStatus.DELIVERED);
        return withStatus(OrderStatus.DELIVERED, at);
    }

    /**
     * Advances (or catch-up jumps) to the lifecycle target derived from {@link #confirmedAt()}.
     * Does not touch stock or payments. No-op when already at target.
     */
    public Order advanceLifecycle(Instant currentTime) {
        Instant at = requireCurrentTime(currentTime);
        if (status == OrderStatus.DELIVERED || status == OrderStatus.CANCELLED) {
            throw new InvalidOrderStateTransitionException(status, status);
        }
        requireNonNull(confirmedAt, "confirmedAt");
        OrderStatus target = targetStatusAt(confirmedAt, at);
        if (target == status) {
            return this;
        }
        if (lifecycleRank(target) <= lifecycleRank(status)) {
            throw new InvalidOrderStateTransitionException(status, target);
        }
        return withStatus(target, at);
    }

    public Order cancel(Instant currentTime) {
        Instant at = requireCurrentTime(currentTime);
        requireTransition(OrderStatus.CANCELLED);
        requireNonNull(confirmedAt, "confirmedAt");
        Instant deadline = confirmedAt.plus(CUSTOMER_CANCELLATION_WINDOW);
        if (!at.isBefore(deadline)) {
            throw new OrderCancellationNotAllowedException("customer cancellation window has expired");
        }
        return cancelledAt(at);
    }

    /**
     * System cleanup for storefront-preview temporary customers. Forces CANCELLED for any
     * non-terminal in-progress order without the customer 2-minute window. Not a customer cancel.
     */
    public Order cancelForCleanup(Instant currentTime) {
        Instant at = requireCurrentTime(currentTime);
        if (status == OrderStatus.CANCELLED || status == OrderStatus.DELIVERED) {
            throw new InvalidOrderStateTransitionException(status, OrderStatus.CANCELLED);
        }
        return cancelledAt(at);
    }

    public boolean isCustomerCancellable(Instant currentTime) {
        requireNonNull(currentTime, "currentTime");
        if (status != OrderStatus.CONFIRMED || confirmedAt == null) {
            return false;
        }
        return currentTime.isBefore(confirmedAt.plus(CUSTOMER_CANCELLATION_WINDOW));
    }

    private Order withStatus(OrderStatus newStatus, Instant at) {
        return of(
                id,
                orderNumber,
                customerId,
                newStatus,
                items,
                shippingAddress,
                paymentId,
                createdAt,
                confirmedAt,
                null,
                at);
    }

    private Order cancelledAt(Instant at) {
        return of(
                id,
                orderNumber,
                customerId,
                OrderStatus.CANCELLED,
                items,
                shippingAddress,
                paymentId,
                createdAt,
                null,
                at,
                at);
    }

    public UUID id() {
        return id;
    }

    public OrderNumber orderNumber() {
        return orderNumber;
    }

    public UUID customerId() {
        return customerId;
    }

    public OrderStatus status() {
        return status;
    }

    public List<OrderItem> items() {
        return items;
    }

    public Money subtotal() {
        return subtotal;
    }

    public Money total() {
        return total;
    }

    public ShippingAddressSnapshot shippingAddress() {
        return shippingAddress;
    }

    public UUID paymentId() {
        return paymentId;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant confirmedAt() {
        return confirmedAt;
    }

    public Instant cancelledAt() {
        return cancelledAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    private static Order of(
            UUID id,
            OrderNumber orderNumber,
            UUID customerId,
            OrderStatus status,
            List<OrderItem> items,
            ShippingAddressSnapshot shippingAddress,
            UUID paymentId,
            Instant createdAt,
            Instant confirmedAt,
            Instant cancelledAt,
            Instant updatedAt) {
        requireStaticNonNull(id, "id");
        requireStaticNonNull(orderNumber, "orderNumber");
        requireStaticNonNull(customerId, "customerId");
        requireStaticNonNull(status, "status");
        requireStaticNonNull(shippingAddress, "shippingAddress");
        requireStaticNonNull(createdAt, "createdAt");
        requireStaticNonNull(updatedAt, "updatedAt");
        if (createdAt.isAfter(updatedAt)) {
            throw new InvalidOrderException("createdAt must not be after updatedAt");
        }

        List<OrderItem> snapshot = copyItems(items);
        Money computedSubtotal = sumSubtotals(snapshot);
        if (status == OrderStatus.CANCELLED) {
            requireStaticNonNull(cancelledAt, "cancelledAt");
            if (confirmedAt != null) {
                throw new InvalidOrderException("cancelled order cannot have confirmedAt");
            }
        } else if (cancelledAt != null) {
            throw new InvalidOrderException("cancelledAt can only exist when the order is cancelled");
        }
        if (status != OrderStatus.CANCELLED) {
            requireStaticNonNull(confirmedAt, "confirmedAt");
        }

        return new Order(
                id,
                orderNumber,
                customerId,
                status,
                snapshot,
                computedSubtotal,
                computedSubtotal,
                shippingAddress,
                paymentId,
                createdAt,
                confirmedAt,
                cancelledAt,
                updatedAt);
    }

    private static int lifecycleRank(OrderStatus status) {
        return switch (status) {
            case CONFIRMED -> 0;
            case PREPARING -> 1;
            case DELIVERY -> 2;
            case DELIVERED -> 3;
            case CANCELLED -> -1;
        };
    }

    private static List<OrderItem> copyItems(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            throw new InvalidOrderException("order must contain at least one item");
        }
        List<OrderItem> copy = new ArrayList<>();
        for (OrderItem item : items) {
            if (item == null) {
                throw new InvalidOrderException("order items cannot contain null");
            }
            copy.add(item);
        }
        return List.copyOf(copy);
    }

    private static Money sumSubtotals(List<OrderItem> items) {
        BigDecimal sum = BigDecimal.ZERO;
        String currency = null;
        for (OrderItem item : items) {
            Money itemSubtotal = item.subtotal();
            if (currency == null) {
                currency = itemSubtotal.currency();
            } else if (!currency.equals(itemSubtotal.currency())) {
                throw new InvalidOrderException("order items must use the same currency");
            }
            sum = sum.add(itemSubtotal.amount());
        }
        return new Money(sum, currency);
    }

    private Instant requireCurrentTime(Instant currentTime) {
        requireNonNull(currentTime, "currentTime");
        if (currentTime.isBefore(createdAt)) {
            throw new InvalidOrderException("currentTime must not be before createdAt");
        }
        return currentTime;
    }

    private void requireTransition(OrderStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new InvalidOrderStateTransitionException(status, target);
        }
    }

    private void requireNonNull(Object value, String field) {
        requireStaticNonNull(value, field);
    }

    private static void requireStaticNonNull(Object value, String field) {
        if (value == null) {
            throw new InvalidOrderException(field + " cannot be null");
        }
    }
}
