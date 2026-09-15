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

    public static final Duration CUSTOMER_CANCELLATION_WINDOW = Duration.ofMinutes(15);

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
                OrderStatus.PENDING,
                items,
                shippingAddress,
                paymentId,
                createdAt,
                null,
                null,
                updatedAt);
    }

    public Order confirm(Instant currentTime) {
        Instant at = requireCurrentTime(currentTime);
        requireTransition(OrderStatus.CONFIRMED);
        return of(
                id,
                orderNumber,
                customerId,
                OrderStatus.CONFIRMED,
                items,
                shippingAddress,
                paymentId,
                createdAt,
                at,
                null,
                at);
    }

    public Order startPreparation(Instant currentTime) {
        Instant at = requireCurrentTime(currentTime);
        requireTransition(OrderStatus.PREPARING);
        return of(
                id,
                orderNumber,
                customerId,
                OrderStatus.PREPARING,
                items,
                shippingAddress,
                paymentId,
                createdAt,
                confirmedAt,
                null,
                at);
    }

    public Order markReady(Instant currentTime) {
        Instant at = requireCurrentTime(currentTime);
        requireTransition(OrderStatus.READY);
        return of(
                id,
                orderNumber,
                customerId,
                OrderStatus.READY,
                items,
                shippingAddress,
                paymentId,
                createdAt,
                confirmedAt,
                null,
                at);
    }

    public Order markDelivered(Instant currentTime) {
        Instant at = requireCurrentTime(currentTime);
        requireTransition(OrderStatus.DELIVERED);
        return of(
                id,
                orderNumber,
                customerId,
                OrderStatus.DELIVERED,
                items,
                shippingAddress,
                paymentId,
                createdAt,
                confirmedAt,
                null,
                at);
    }

    public Order cancel(Instant currentTime) {
        Instant at = requireCurrentTime(currentTime);
        requireTransition(OrderStatus.CANCELLED);
        Instant deadline = createdAt.plus(CUSTOMER_CANCELLATION_WINDOW);
        if (at.isAfter(deadline)) {
            throw new OrderCancellationNotAllowedException("customer cancellation window has expired");
        }
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

    public boolean isEligibleForAutomaticConfirmation(Instant currentTime) {
        requireNonNull(currentTime, "currentTime");
        return status == OrderStatus.PENDING
                && currentTime.isAfter(createdAt.plus(CUSTOMER_CANCELLATION_WINDOW));
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
        requireNonNull(id, "id");
        requireNonNull(orderNumber, "orderNumber");
        requireNonNull(customerId, "customerId");
        requireNonNull(status, "status");
        requireNonNull(shippingAddress, "shippingAddress");
        requireNonNull(createdAt, "createdAt");
        requireNonNull(updatedAt, "updatedAt");
        if (createdAt.isAfter(updatedAt)) {
            throw new InvalidOrderException("createdAt must not be after updatedAt");
        }

        List<OrderItem> snapshot = copyItems(items);
        Money computedSubtotal = sumSubtotals(snapshot);
        if (status == OrderStatus.CANCELLED) {
            requireNonNull(cancelledAt, "cancelledAt");
            if (confirmedAt != null) {
                throw new InvalidOrderException("cancelled order cannot have confirmedAt");
            }
        } else if (cancelledAt != null) {
            throw new InvalidOrderException("cancelledAt can only exist when the order is cancelled");
        }
        if (status == OrderStatus.PENDING) {
            if (confirmedAt != null) {
                throw new InvalidOrderException("confirmedAt can only exist when the order is confirmed");
            }
        } else if (status != OrderStatus.CANCELLED) {
            requireNonNull(confirmedAt, "confirmedAt");
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

    private static void requireNonNull(Object value, String field) {
        if (value == null) {
            throw new InvalidOrderException(field + " cannot be null");
        }
    }
}
