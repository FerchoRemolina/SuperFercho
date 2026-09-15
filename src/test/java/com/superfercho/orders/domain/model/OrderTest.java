package com.superfercho.orders.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.superfercho.orders.domain.exception.InvalidOrderException;
import com.superfercho.orders.domain.exception.InvalidOrderStateTransitionException;
import com.superfercho.orders.domain.exception.OrderCancellationNotAllowedException;
import com.superfercho.platform.money.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrderTest {

    private static final UUID ORDER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID CUSTOMER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID PRODUCT_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID PAYMENT_ID = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
    private static final Instant CREATED_AT = Instant.parse("2026-01-15T12:00:00Z");
    private static final Instant CANCELLATION_DEADLINE = CREATED_AT.plus(Order.CUSTOMER_CANCELLATION_WINDOW);
    private static final Instant AFTER_DEADLINE = Instant.parse("2026-01-15T12:15:00.001Z");

    @Test
    void shouldCreateValidPendingOrder() {
        OrderItem item = milk(2);
        Order order = validOrder().items(List.of(item)).build();

        assertEquals(ORDER_ID, order.id());
        assertEquals("ORD-1001", order.orderNumber().value());
        assertEquals(CUSTOMER_ID, order.customerId());
        assertEquals(OrderStatus.PENDING, order.status());
        assertEquals(1, order.items().size());
        assertEquals(item, order.items().get(0));
        assertEquals(Money.cop(new BigDecimal("21.00")), order.subtotal());
        assertEquals(order.subtotal(), order.total());
        assertEquals("Ada Lovelace", order.shippingAddress().recipientName());
        assertEquals("Calle 1 # 2-3", order.shippingAddress().addressLine());
        assertNull(order.paymentId());
        assertEquals(CREATED_AT, order.createdAt());
        assertNull(order.confirmedAt());
        assertNull(order.cancelledAt());
        assertEquals(CREATED_AT, order.updatedAt());
    }

    @Test
    void shouldComputeTotalAsSumOfItemSubtotals() {
        Order order = validOrder()
                .items(List.of(milk(2), OrderItem.create(
                        UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd"),
                        PRODUCT_ID,
                        "Pan",
                        Money.cop(new BigDecimal("3.00")),
                        4)))
                .build();

        assertEquals(Money.cop(new BigDecimal("33.00")), order.subtotal());
        assertEquals(order.subtotal(), order.total());
    }

    @Test
    void shouldRejectOrderWithoutItems() {
        assertThrows(InvalidOrderException.class, () -> validOrder().items(List.of()).build());
        assertThrows(InvalidOrderException.class, () -> validOrder().items(null).build());
    }

    @Test
    void shouldRejectOrderWhenCustomerIdIsNull() {
        assertThrows(InvalidOrderException.class, () -> validOrder().customerId(null).build());
    }

    @Test
    void shouldRejectOrderWhenShippingAddressIsNull() {
        assertThrows(InvalidOrderException.class, () -> validOrder().shippingAddress(null).build());
    }

    @Test
    void shouldRejectInvalidShippingAddressSnapshot() {
        assertThrows(
                InvalidOrderException.class,
                () -> new ShippingAddressSnapshot(" ", "Calle 1", null, "Bogotá", "Cundinamarca", "3001234567"));
    }

    @Test
    void shouldKeepItemSnapshotIndependentFromOriginalList() {
        OrderItem item = milk(1);
        List<OrderItem> items = new ArrayList<>();
        items.add(item);

        Order order = validOrder().items(items).build();
        items.clear();

        assertEquals(1, order.items().size());
        assertEquals(item, order.items().get(0));
    }

    @Test
    void shouldExposeItemsAsUnmodifiableCollection() {
        Order order = validOrder().build();

        assertThrows(UnsupportedOperationException.class, () -> order.items().add(milk(1)));
        assertThrows(UnsupportedOperationException.class, () -> order.items().clear());
    }

    @Test
    void shouldAllowNullAdditionalInfoOnShippingAddress() {
        ShippingAddressSnapshot snapshot = new ShippingAddressSnapshot(
                "Ada Lovelace", "Calle 1 # 2-3", null, "Bogotá", "Cundinamarca", "3001234567");

        Order order = validOrder().shippingAddress(snapshot).build();

        assertNull(order.shippingAddress().additionalInfo());
    }

    @Test
    void shouldKeepShippingAddressSnapshotImmutable() {
        ShippingAddressSnapshot snapshot = validAddress();
        Order order = validOrder().shippingAddress(snapshot).build();

        assertEquals(snapshot, order.shippingAddress());
        assertEquals("Ada Lovelace", order.shippingAddress().recipientName());
    }

    @Test
    void shouldRejectBlankOrderNumber() {
        assertThrows(InvalidOrderException.class, () -> new OrderNumber("  "));
    }

    @Test
    void shouldConfirmPendingOrder() {
        Instant confirmedAt = Instant.parse("2026-01-15T12:05:00Z");
        Order confirmed = validOrder().build().confirm(confirmedAt);

        assertEquals(OrderStatus.CONFIRMED, confirmed.status());
        assertEquals(confirmedAt, confirmed.confirmedAt());
        assertEquals(confirmedAt, confirmed.updatedAt());
        assertEquals(CREATED_AT, confirmed.createdAt());
        assertNull(confirmed.cancelledAt());
    }

    @Test
    void shouldCancelPendingOrderWithinWindow() {
        Instant cancelledAt = Instant.parse("2026-01-15T12:10:00Z");
        Order cancelled = validOrder().build().cancel(cancelledAt);

        assertEquals(OrderStatus.CANCELLED, cancelled.status());
        assertEquals(cancelledAt, cancelled.cancelledAt());
        assertEquals(cancelledAt, cancelled.updatedAt());
        assertNull(cancelled.confirmedAt());
    }

    @Test
    void shouldCancelPendingOrderAtCreatedAt() {
        Order cancelled = validOrder().build().cancel(CREATED_AT);

        assertEquals(OrderStatus.CANCELLED, cancelled.status());
        assertEquals(CREATED_AT, cancelled.cancelledAt());
    }

    @Test
    void shouldCancelPendingOrderExactlyAtDeadline() {
        Order cancelled = validOrder().build().cancel(CANCELLATION_DEADLINE);

        assertEquals(OrderStatus.CANCELLED, cancelled.status());
        assertEquals(CANCELLATION_DEADLINE, cancelled.cancelledAt());
        assertEquals(CANCELLATION_DEADLINE, cancelled.updatedAt());
    }

    @Test
    void shouldRejectCancellationAfterDeadline() {
        Order order = validOrder().build();

        assertThrows(OrderCancellationNotAllowedException.class, () -> order.cancel(AFTER_DEADLINE));
        assertEquals(OrderStatus.PENDING, order.status());
    }

    @Test
    void shouldRejectCancellationFromConfirmedOrder() {
        Order confirmed = validOrder().build().confirm(Instant.parse("2026-01-15T12:01:00Z"));

        assertThrows(
                InvalidOrderStateTransitionException.class,
                () -> confirmed.cancel(Instant.parse("2026-01-15T12:02:00Z")));
    }

    @Test
    void shouldProgressConfirmedOrderToDelivered() {
        Instant t1 = Instant.parse("2026-01-15T12:16:00Z");
        Instant t2 = Instant.parse("2026-01-15T12:20:00Z");
        Instant t3 = Instant.parse("2026-01-15T12:30:00Z");
        Instant t4 = Instant.parse("2026-01-15T13:00:00Z");

        Order delivered = validOrder()
                .build()
                .confirm(t1)
                .startPreparation(t2)
                .markReady(t3)
                .markDelivered(t4);

        assertEquals(OrderStatus.DELIVERED, delivered.status());
        assertEquals(t1, delivered.confirmedAt());
        assertEquals(t4, delivered.updatedAt());
        assertNull(delivered.cancelledAt());
    }

    @Test
    void shouldRejectPendingToPreparing() {
        assertThrows(
                InvalidOrderStateTransitionException.class,
                () -> validOrder().build().startPreparation(Instant.parse("2026-01-15T12:01:00Z")));
    }

    @Test
    void shouldRejectConfirmedToDelivered() {
        Order confirmed = validOrder().build().confirm(Instant.parse("2026-01-15T12:01:00Z"));

        assertThrows(
                InvalidOrderStateTransitionException.class,
                () -> confirmed.markDelivered(Instant.parse("2026-01-15T12:02:00Z")));
    }

    @Test
    void shouldRejectPreparingToCancelled() {
        Order preparing = validOrder()
                .build()
                .confirm(Instant.parse("2026-01-15T12:01:00Z"))
                .startPreparation(Instant.parse("2026-01-15T12:02:00Z"));

        assertThrows(
                InvalidOrderStateTransitionException.class,
                () -> preparing.cancel(Instant.parse("2026-01-15T12:03:00Z")));
    }

    @Test
    void shouldRejectDeliveredToAnyFurtherTransition() {
        Order delivered = validOrder()
                .build()
                .confirm(Instant.parse("2026-01-15T12:16:00Z"))
                .startPreparation(Instant.parse("2026-01-15T12:17:00Z"))
                .markReady(Instant.parse("2026-01-15T12:18:00Z"))
                .markDelivered(Instant.parse("2026-01-15T12:19:00Z"));

        assertThrows(
                InvalidOrderStateTransitionException.class,
                () -> delivered.markReady(Instant.parse("2026-01-15T12:20:00Z")));
        assertThrows(
                InvalidOrderStateTransitionException.class,
                () -> delivered.cancel(Instant.parse("2026-01-15T12:20:00Z")));
    }

    @Test
    void shouldNotBeEligibleForAutomaticConfirmationWithinWindow() {
        Order order = validOrder().build();

        assertFalse(order.isEligibleForAutomaticConfirmation(CREATED_AT));
        assertFalse(order.isEligibleForAutomaticConfirmation(Instant.parse("2026-01-15T12:14:59Z")));
    }

    @Test
    void shouldNotBeEligibleForAutomaticConfirmationExactlyAtDeadline() {
        assertFalse(validOrder().build().isEligibleForAutomaticConfirmation(CANCELLATION_DEADLINE));
    }

    @Test
    void shouldBeEligibleForAutomaticConfirmationAfterDeadline() {
        assertTrue(validOrder().build().isEligibleForAutomaticConfirmation(AFTER_DEADLINE));
    }

    @Test
    void shouldNotBeEligibleForAutomaticConfirmationWhenNotPending() {
        Order confirmed = validOrder().build().confirm(AFTER_DEADLINE);

        assertFalse(confirmed.isEligibleForAutomaticConfirmation(AFTER_DEADLINE.plusSeconds(60)));
    }

    @Test
    void shouldPreservePaymentIdWhenPresent() {
        UUID paymentId = PAYMENT_ID;
        Order order = validOrder().paymentId(paymentId).build();

        assertEquals(paymentId, order.paymentId());
        assertEquals(paymentId, order.confirm(Instant.parse("2026-01-15T12:01:00Z")).paymentId());
    }

    @Test
    void shouldReconstitutePendingOrder() {
        Order reconstituted = reconstitute(
                OrderStatus.PENDING, PAYMENT_ID, CREATED_AT, null, null, CREATED_AT);

        assertReconstitutedIdentity(reconstituted);
        assertEquals(OrderStatus.PENDING, reconstituted.status());
        assertEquals(CREATED_AT, reconstituted.createdAt());
        assertEquals(CREATED_AT, reconstituted.updatedAt());
        assertNull(reconstituted.confirmedAt());
        assertNull(reconstituted.cancelledAt());
        assertEquals(PAYMENT_ID, reconstituted.paymentId());
    }

    @Test
    void shouldReconstituteConfirmedOrderPreservingDistinctUpdatedAt() {
        Instant updatedAt = Instant.parse("2026-01-15T12:20:00Z");
        Instant confirmedAt = Instant.parse("2026-01-15T12:16:00Z");
        Order reconstituted = reconstitute(
                OrderStatus.CONFIRMED, PAYMENT_ID, CREATED_AT, confirmedAt, null, updatedAt);

        assertEquals(OrderStatus.CONFIRMED, reconstituted.status());
        assertEquals(confirmedAt, reconstituted.confirmedAt());
        assertEquals(updatedAt, reconstituted.updatedAt());
        assertNull(reconstituted.cancelledAt());
        assertEquals(PAYMENT_ID, reconstituted.paymentId());
    }

    @Test
    void shouldReconstitutePreparingReadyAndDeliveredOrders() {
        Instant confirmedAt = Instant.parse("2026-01-15T12:16:00Z");
        Instant updatedAt = Instant.parse("2026-01-15T13:00:00Z");

        Order preparing =
                reconstitute(OrderStatus.PREPARING, PAYMENT_ID, CREATED_AT, confirmedAt, null, updatedAt);
        Order ready = reconstitute(OrderStatus.READY, PAYMENT_ID, CREATED_AT, confirmedAt, null, updatedAt);
        Order delivered =
                reconstitute(OrderStatus.DELIVERED, PAYMENT_ID, CREATED_AT, confirmedAt, null, updatedAt);

        assertEquals(OrderStatus.PREPARING, preparing.status());
        assertEquals(OrderStatus.READY, ready.status());
        assertEquals(OrderStatus.DELIVERED, delivered.status());
        assertEquals(confirmedAt, preparing.confirmedAt());
        assertEquals(updatedAt, preparing.updatedAt());
        assertEquals(confirmedAt, ready.confirmedAt());
        assertEquals(updatedAt, ready.updatedAt());
        assertEquals(confirmedAt, delivered.confirmedAt());
        assertEquals(updatedAt, delivered.updatedAt());
        assertNull(delivered.cancelledAt());
    }

    @Test
    void shouldReconstituteCancelledOrder() {
        Instant cancelledAt = Instant.parse("2026-01-15T12:10:00Z");
        Order reconstituted = reconstitute(
                OrderStatus.CANCELLED, PAYMENT_ID, CREATED_AT, null, cancelledAt, cancelledAt);

        assertEquals(OrderStatus.CANCELLED, reconstituted.status());
        assertEquals(cancelledAt, reconstituted.cancelledAt());
        assertEquals(cancelledAt, reconstituted.updatedAt());
        assertNull(reconstituted.confirmedAt());
        assertEquals(PAYMENT_ID, reconstituted.paymentId());
        assertEquals(validAddress(), reconstituted.shippingAddress());
        assertEquals(milk(2), reconstituted.items().get(0));
    }

    @Test
    void shouldRejectReconstitutePendingWithConfirmedAt() {
        assertThrows(
                InvalidOrderException.class,
                () -> reconstitute(
                        OrderStatus.PENDING,
                        PAYMENT_ID,
                        CREATED_AT,
                        Instant.parse("2026-01-15T12:01:00Z"),
                        null,
                        CREATED_AT));
    }

    @Test
    void shouldRejectReconstituteCancelledWithoutCancelledAt() {
        assertThrows(
                InvalidOrderException.class,
                () -> reconstitute(OrderStatus.CANCELLED, PAYMENT_ID, CREATED_AT, null, null, CREATED_AT));
    }

    @Test
    void shouldRejectReconstituteCancelledWithConfirmedAt() {
        assertThrows(
                InvalidOrderException.class,
                () -> reconstitute(
                        OrderStatus.CANCELLED,
                        PAYMENT_ID,
                        CREATED_AT,
                        Instant.parse("2026-01-15T12:01:00Z"),
                        Instant.parse("2026-01-15T12:10:00Z"),
                        Instant.parse("2026-01-15T12:10:00Z")));
    }

    @Test
    void shouldRejectReconstituteConfirmedWithoutConfirmedAt() {
        assertThrows(
                InvalidOrderException.class,
                () -> reconstitute(OrderStatus.CONFIRMED, PAYMENT_ID, CREATED_AT, null, null, CREATED_AT));
    }

    @Test
    void shouldRejectReconstituteWhenCreatedAtAfterUpdatedAt() {
        assertThrows(
                InvalidOrderException.class,
                () -> reconstitute(
                        OrderStatus.PENDING,
                        PAYMENT_ID,
                        Instant.parse("2026-01-15T12:10:00Z"),
                        null,
                        null,
                        CREATED_AT));
    }

    @Test
    void shouldRejectReconstituteWithoutItems() {
        assertThrows(
                InvalidOrderException.class,
                () -> Order.reconstitute(
                        ORDER_ID,
                        new OrderNumber("ORD-1001"),
                        CUSTOMER_ID,
                        OrderStatus.PENDING,
                        List.of(),
                        validAddress(),
                        PAYMENT_ID,
                        CREATED_AT,
                        null,
                        null,
                        CREATED_AT));
    }

    @Test
    void shouldRejectReconstituteWhenRequiredIdentityIsNull() {
        assertThrows(
                InvalidOrderException.class,
                () -> Order.reconstitute(
                        null,
                        new OrderNumber("ORD-1001"),
                        CUSTOMER_ID,
                        OrderStatus.PENDING,
                        List.of(milk(2)),
                        validAddress(),
                        PAYMENT_ID,
                        CREATED_AT,
                        null,
                        null,
                        CREATED_AT));
        assertThrows(
                InvalidOrderException.class,
                () -> reconstitute(null, PAYMENT_ID, CREATED_AT, null, null, CREATED_AT));
    }

    private static void assertReconstitutedIdentity(Order order) {
        assertEquals(ORDER_ID, order.id());
        assertEquals("ORD-1001", order.orderNumber().value());
        assertEquals(CUSTOMER_ID, order.customerId());
        assertEquals(1, order.items().size());
        assertEquals(milk(2), order.items().get(0));
        assertEquals(validAddress(), order.shippingAddress());
    }

    private static Order reconstitute(
            OrderStatus status,
            UUID paymentId,
            Instant createdAt,
            Instant confirmedAt,
            Instant cancelledAt,
            Instant updatedAt) {
        return Order.reconstitute(
                ORDER_ID,
                new OrderNumber("ORD-1001"),
                CUSTOMER_ID,
                status,
                List.of(milk(2)),
                validAddress(),
                paymentId,
                createdAt,
                confirmedAt,
                cancelledAt,
                updatedAt);
    }

    private static OrderBuilder validOrder() {
        return new OrderBuilder();
    }

    private static OrderItem milk(int quantity) {
        return OrderItem.create(
                UUID.fromString("99999999-9999-9999-9999-999999999999"),
                PRODUCT_ID,
                "Leche entera",
                Money.cop(new BigDecimal("10.50")),
                quantity);
    }

    private static ShippingAddressSnapshot validAddress() {
        return new ShippingAddressSnapshot(
                "Ada Lovelace", "Calle 1 # 2-3", "Apto 101", "Bogotá", "Cundinamarca", "3001234567");
    }

    private static final class OrderBuilder {
        private UUID id = ORDER_ID;
        private OrderNumber orderNumber = new OrderNumber("ORD-1001");
        private UUID customerId = CUSTOMER_ID;
        private List<OrderItem> items = List.of(milk(2));
        private ShippingAddressSnapshot shippingAddress = validAddress();
        private UUID paymentId;
        private Instant createdAt = CREATED_AT;
        private Instant updatedAt = CREATED_AT;

        private OrderBuilder customerId(UUID customerId) {
            this.customerId = customerId;
            return this;
        }

        private OrderBuilder items(List<OrderItem> items) {
            this.items = items;
            return this;
        }

        private OrderBuilder shippingAddress(ShippingAddressSnapshot shippingAddress) {
            this.shippingAddress = shippingAddress;
            return this;
        }

        private OrderBuilder paymentId(UUID paymentId) {
            this.paymentId = paymentId;
            return this;
        }

        private Order build() {
            return Order.create(
                    id, orderNumber, customerId, items, shippingAddress, paymentId, createdAt, updatedAt);
        }
    }
}
