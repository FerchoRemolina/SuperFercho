package com.superfercho.orders.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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
    private static final Instant WITHIN_WINDOW = Instant.parse("2026-01-15T12:01:59.999Z");
    private static final Instant AFTER_DEADLINE = Instant.parse("2026-01-15T12:02:00.001Z");

    @Test
    void shouldCreateConfirmedOrder() {
        OrderItem item = milk(2);
        Order order = validOrder().items(List.of(item)).build();

        assertEquals(ORDER_ID, order.id());
        assertEquals("ORD-1001", order.orderNumber().value());
        assertEquals(CUSTOMER_ID, order.customerId());
        assertEquals(OrderStatus.CONFIRMED, order.status());
        assertEquals(1, order.items().size());
        assertEquals(item, order.items().get(0));
        assertEquals(Money.cop(new BigDecimal("21.00")), order.subtotal());
        assertEquals(order.subtotal(), order.total());
        assertEquals("Ada Lovelace", order.shippingAddress().recipientName());
        assertEquals("Calle 1 # 2-3", order.shippingAddress().addressLine());
        assertNull(order.paymentId());
        assertEquals(CREATED_AT, order.createdAt());
        assertEquals(CREATED_AT, order.confirmedAt());
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
    void shouldResolveTargetStatusFromConfirmedAtAge() {
        assertEquals(OrderStatus.CONFIRMED, Order.targetStatusAt(CREATED_AT, CREATED_AT));
        assertEquals(OrderStatus.CONFIRMED, Order.targetStatusAt(CREATED_AT, CREATED_AT.plusSeconds(119)));
        assertEquals(OrderStatus.PREPARING, Order.targetStatusAt(CREATED_AT, CREATED_AT.plusSeconds(120)));
        assertEquals(OrderStatus.PREPARING, Order.targetStatusAt(CREATED_AT, CREATED_AT.plusSeconds(239)));
        assertEquals(OrderStatus.DELIVERY, Order.targetStatusAt(CREATED_AT, CREATED_AT.plusSeconds(240)));
        assertEquals(OrderStatus.DELIVERY, Order.targetStatusAt(CREATED_AT, CREATED_AT.plusSeconds(359)));
        assertEquals(OrderStatus.DELIVERED, Order.targetStatusAt(CREATED_AT, CREATED_AT.plusSeconds(360)));
        assertEquals(OrderStatus.DELIVERED, Order.targetStatusAt(CREATED_AT, CREATED_AT.plusSeconds(3600)));
    }

    @Test
    void shouldRejectTargetStatusBeforeConfirmedAtOrWithNulls() {
        assertThrows(
                InvalidOrderException.class,
                () -> Order.targetStatusAt(CREATED_AT, CREATED_AT.minusSeconds(1)));
        assertThrows(InvalidOrderException.class, () -> Order.targetStatusAt(null, CREATED_AT));
        assertThrows(InvalidOrderException.class, () -> Order.targetStatusAt(CREATED_AT, null));
    }

    @Test
    void shouldProgressConfirmedOrderToDeliveredStepByStep() {
        Instant t1 = CREATED_AT.plusSeconds(120);
        Instant t2 = CREATED_AT.plusSeconds(240);
        Instant t3 = CREATED_AT.plusSeconds(360);

        Order delivered =
                validOrder().build().startPreparation(t1).startDelivery(t2).markDelivered(t3);

        assertEquals(OrderStatus.DELIVERED, delivered.status());
        assertEquals(CREATED_AT, delivered.confirmedAt());
        assertEquals(t3, delivered.updatedAt());
        assertNull(delivered.cancelledAt());
    }

    @Test
    void shouldAdvanceLifecycleOneStepAtATime() {
        Order confirmed = validOrder().build();

        Order preparing = confirmed.advanceLifecycle(CREATED_AT.plusSeconds(120));
        assertEquals(OrderStatus.PREPARING, preparing.status());
        assertEquals(CREATED_AT.plusSeconds(120), preparing.updatedAt());

        Order delivery = preparing.advanceLifecycle(CREATED_AT.plusSeconds(240));
        assertEquals(OrderStatus.DELIVERY, delivery.status());

        Order delivered = delivery.advanceLifecycle(CREATED_AT.plusSeconds(360));
        assertEquals(OrderStatus.DELIVERED, delivered.status());
        assertEquals(CREATED_AT, delivered.confirmedAt());
        assertNull(delivered.cancelledAt());
    }

    @Test
    void shouldCatchUpLifecycleToTargetStatusInOneJump() {
        Order delivered = validOrder().build().advanceLifecycle(CREATED_AT.plusSeconds(3600));

        assertEquals(OrderStatus.DELIVERED, delivered.status());
        assertEquals(CREATED_AT, delivered.confirmedAt());
    }

    @Test
    void shouldReturnSameInstanceWhenLifecycleTargetIsCurrentStatus() {
        Order confirmed = validOrder().build();

        assertSame(confirmed, confirmed.advanceLifecycle(CREATED_AT.plusSeconds(60)));
    }

    @Test
    void shouldRejectAdvanceLifecycleFromTerminalStatuses() {
        Order delivered = reconstitute(
                OrderStatus.DELIVERED, PAYMENT_ID, CREATED_AT, CREATED_AT, null, CREATED_AT.plusSeconds(360));
        Order cancelled = reconstitute(
                OrderStatus.CANCELLED, PAYMENT_ID, CREATED_AT, null, CREATED_AT, CREATED_AT);

        assertThrows(
                InvalidOrderStateTransitionException.class,
                () -> delivered.advanceLifecycle(CREATED_AT.plusSeconds(3600)));
        assertThrows(
                InvalidOrderStateTransitionException.class,
                () -> cancelled.advanceLifecycle(CREATED_AT.plusSeconds(3600)));
    }

    @Test
    void shouldCancelConfirmedOrderWithinWindow() {
        Order cancelled = validOrder().build().cancel(WITHIN_WINDOW);

        assertEquals(OrderStatus.CANCELLED, cancelled.status());
        assertEquals(WITHIN_WINDOW, cancelled.cancelledAt());
        assertEquals(WITHIN_WINDOW, cancelled.updatedAt());
        assertNull(cancelled.confirmedAt());
    }

    @Test
    void shouldCancelConfirmedOrderAtCreatedAt() {
        Order cancelled = validOrder().build().cancel(CREATED_AT);

        assertEquals(OrderStatus.CANCELLED, cancelled.status());
        assertEquals(CREATED_AT, cancelled.cancelledAt());
    }

    @Test
    void shouldRejectCancellationExactlyAtDeadline() {
        Order order = validOrder().build();

        assertThrows(OrderCancellationNotAllowedException.class, () -> order.cancel(CANCELLATION_DEADLINE));
        assertEquals(OrderStatus.CONFIRMED, order.status());
    }

    @Test
    void shouldRejectCancellationAfterDeadline() {
        Order order = validOrder().build();

        assertThrows(OrderCancellationNotAllowedException.class, () -> order.cancel(AFTER_DEADLINE));
        assertEquals(OrderStatus.CONFIRMED, order.status());
    }

    @Test
    void shouldRejectCancellationFromPreparing() {
        Order preparing = validOrder().build().startPreparation(CREATED_AT.plusSeconds(120));

        assertThrows(
                InvalidOrderStateTransitionException.class,
                () -> preparing.cancel(CREATED_AT.plusSeconds(121)));
    }

    @Test
    void shouldRejectCancellationFromDeliveryAndDelivered() {
        Order delivery = validOrder()
                .build()
                .startPreparation(CREATED_AT.plusSeconds(120))
                .startDelivery(CREATED_AT.plusSeconds(240));
        Order delivered = delivery.markDelivered(CREATED_AT.plusSeconds(360));

        assertThrows(
                InvalidOrderStateTransitionException.class,
                () -> delivery.cancel(CREATED_AT.plusSeconds(241)));
        assertThrows(
                InvalidOrderStateTransitionException.class,
                () -> delivered.cancel(CREATED_AT.plusSeconds(361)));
    }

    @Test
    void shouldForceCleanupCancellationFromAnyInProgressStatusWithoutWindow() {
        Order confirmed = validOrder().build();
        Order preparing = confirmed.startPreparation(CREATED_AT.plusSeconds(120));
        Order delivery = preparing.startDelivery(CREATED_AT.plusSeconds(240));
        Instant longAfterWindow = CREATED_AT.plusSeconds(3600);

        for (Order order : List.of(confirmed, preparing, delivery)) {
            Order cancelled = order.cancelForCleanup(longAfterWindow);

            assertEquals(OrderStatus.CANCELLED, cancelled.status());
            assertEquals(longAfterWindow, cancelled.cancelledAt());
            assertEquals(longAfterWindow, cancelled.updatedAt());
            assertNull(cancelled.confirmedAt());
        }
    }

    @Test
    void shouldRejectCleanupCancellationFromTerminalStatuses() {
        Order delivered = validOrder()
                .build()
                .startPreparation(CREATED_AT.plusSeconds(120))
                .startDelivery(CREATED_AT.plusSeconds(240))
                .markDelivered(CREATED_AT.plusSeconds(360));
        Order cancelled = validOrder().build().cancel(CREATED_AT);

        assertThrows(
                InvalidOrderStateTransitionException.class,
                () -> delivered.cancelForCleanup(CREATED_AT.plusSeconds(3600)));
        assertThrows(
                InvalidOrderStateTransitionException.class,
                () -> cancelled.cancelForCleanup(CREATED_AT.plusSeconds(3600)));
    }

    @Test
    void shouldReportCustomerCancellableOnlyForConfirmedWithinWindow() {
        Order confirmed = validOrder().build();

        assertTrue(confirmed.isCustomerCancellable(CREATED_AT));
        assertTrue(confirmed.isCustomerCancellable(WITHIN_WINDOW));
        assertFalse(confirmed.isCustomerCancellable(CANCELLATION_DEADLINE));
        assertFalse(confirmed.isCustomerCancellable(AFTER_DEADLINE));
        assertFalse(confirmed
                .startPreparation(CREATED_AT.plusSeconds(120))
                .isCustomerCancellable(CREATED_AT.plusSeconds(120)));
        assertFalse(confirmed.cancel(CREATED_AT).isCustomerCancellable(CREATED_AT));
    }

    @Test
    void shouldRejectConfirmedToDelivery() {
        Order confirmed = validOrder().build();

        assertThrows(
                InvalidOrderStateTransitionException.class,
                () -> confirmed.startDelivery(CREATED_AT.plusSeconds(120)));
        assertThrows(
                InvalidOrderStateTransitionException.class,
                () -> confirmed.markDelivered(CREATED_AT.plusSeconds(120)));
    }

    @Test
    void shouldRejectDeliveredToAnyFurtherTransition() {
        Order delivered = validOrder()
                .build()
                .startPreparation(CREATED_AT.plusSeconds(120))
                .startDelivery(CREATED_AT.plusSeconds(240))
                .markDelivered(CREATED_AT.plusSeconds(360));

        assertThrows(
                InvalidOrderStateTransitionException.class,
                () -> delivered.startDelivery(CREATED_AT.plusSeconds(400)));
        assertThrows(
                InvalidOrderStateTransitionException.class,
                () -> delivered.startPreparation(CREATED_AT.plusSeconds(400)));
        assertThrows(
                InvalidOrderStateTransitionException.class,
                () -> delivered.markDelivered(CREATED_AT.plusSeconds(400)));
    }

    @Test
    void shouldRejectTransitionsBeforeCreatedAt() {
        Order order = validOrder().build();

        assertThrows(
                InvalidOrderException.class, () -> order.startPreparation(CREATED_AT.minusSeconds(1)));
        assertThrows(InvalidOrderException.class, () -> order.cancel(CREATED_AT.minusSeconds(1)));
        assertThrows(InvalidOrderException.class, () -> order.cancelForCleanup(CREATED_AT.minusSeconds(1)));
        assertThrows(InvalidOrderException.class, () -> order.advanceLifecycle(CREATED_AT.minusSeconds(1)));
    }

    @Test
    void shouldPreservePaymentIdWhenPresent() {
        Order order = validOrder().paymentId(PAYMENT_ID).build();

        assertEquals(PAYMENT_ID, order.paymentId());
        assertEquals(PAYMENT_ID, order.startPreparation(CREATED_AT.plusSeconds(120)).paymentId());
        assertEquals(PAYMENT_ID, order.cancel(CREATED_AT).paymentId());
    }

    @Test
    void shouldReconstituteConfirmedOrderPreservingDistinctUpdatedAt() {
        Instant updatedAt = Instant.parse("2026-01-15T12:01:00Z");
        Order reconstituted =
                reconstitute(OrderStatus.CONFIRMED, PAYMENT_ID, CREATED_AT, CREATED_AT, null, updatedAt);

        assertReconstitutedIdentity(reconstituted);
        assertEquals(OrderStatus.CONFIRMED, reconstituted.status());
        assertEquals(CREATED_AT, reconstituted.confirmedAt());
        assertEquals(updatedAt, reconstituted.updatedAt());
        assertNull(reconstituted.cancelledAt());
        assertEquals(PAYMENT_ID, reconstituted.paymentId());
    }

    @Test
    void shouldReconstitutePreparingDeliveryAndDeliveredOrders() {
        Instant confirmedAt = CREATED_AT;
        Instant updatedAt = Instant.parse("2026-01-15T13:00:00Z");

        Order preparing =
                reconstitute(OrderStatus.PREPARING, PAYMENT_ID, CREATED_AT, confirmedAt, null, updatedAt);
        Order delivery =
                reconstitute(OrderStatus.DELIVERY, PAYMENT_ID, CREATED_AT, confirmedAt, null, updatedAt);
        Order delivered =
                reconstitute(OrderStatus.DELIVERED, PAYMENT_ID, CREATED_AT, confirmedAt, null, updatedAt);

        assertEquals(OrderStatus.PREPARING, preparing.status());
        assertEquals(OrderStatus.DELIVERY, delivery.status());
        assertEquals(OrderStatus.DELIVERED, delivered.status());
        assertEquals(confirmedAt, preparing.confirmedAt());
        assertEquals(confirmedAt, delivery.confirmedAt());
        assertEquals(confirmedAt, delivered.confirmedAt());
        assertEquals(updatedAt, delivered.updatedAt());
        assertNull(delivered.cancelledAt());
    }

    @Test
    void shouldReconstituteCancelledOrder() {
        Instant cancelledAt = Instant.parse("2026-01-15T12:01:00Z");
        Order reconstituted =
                reconstitute(OrderStatus.CANCELLED, PAYMENT_ID, CREATED_AT, null, cancelledAt, cancelledAt);

        assertEquals(OrderStatus.CANCELLED, reconstituted.status());
        assertEquals(cancelledAt, reconstituted.cancelledAt());
        assertEquals(cancelledAt, reconstituted.updatedAt());
        assertNull(reconstituted.confirmedAt());
        assertEquals(PAYMENT_ID, reconstituted.paymentId());
        assertEquals(validAddress(), reconstituted.shippingAddress());
        assertEquals(milk(2), reconstituted.items().get(0));
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
                        CREATED_AT,
                        Instant.parse("2026-01-15T12:01:00Z"),
                        Instant.parse("2026-01-15T12:01:00Z")));
    }

    @Test
    void shouldRejectReconstituteInProgressWithoutConfirmedAt() {
        for (OrderStatus status :
                List.of(OrderStatus.CONFIRMED, OrderStatus.PREPARING, OrderStatus.DELIVERY, OrderStatus.DELIVERED)) {
            assertThrows(
                    InvalidOrderException.class,
                    () -> reconstitute(status, PAYMENT_ID, CREATED_AT, null, null, CREATED_AT));
        }
    }

    @Test
    void shouldRejectReconstituteInProgressWithCancelledAt() {
        assertThrows(
                InvalidOrderException.class,
                () -> reconstitute(
                        OrderStatus.CONFIRMED, PAYMENT_ID, CREATED_AT, CREATED_AT, CREATED_AT, CREATED_AT));
    }

    @Test
    void shouldRejectReconstituteWhenCreatedAtAfterUpdatedAt() {
        assertThrows(
                InvalidOrderException.class,
                () -> reconstitute(
                        OrderStatus.CONFIRMED,
                        PAYMENT_ID,
                        Instant.parse("2026-01-15T12:10:00Z"),
                        Instant.parse("2026-01-15T12:10:00Z"),
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
                        OrderStatus.CONFIRMED,
                        List.of(),
                        validAddress(),
                        PAYMENT_ID,
                        CREATED_AT,
                        CREATED_AT,
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
                        OrderStatus.CONFIRMED,
                        List.of(milk(2)),
                        validAddress(),
                        PAYMENT_ID,
                        CREATED_AT,
                        CREATED_AT,
                        null,
                        CREATED_AT));
        assertThrows(
                InvalidOrderException.class,
                () -> reconstitute(null, PAYMENT_ID, CREATED_AT, CREATED_AT, null, CREATED_AT));
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
