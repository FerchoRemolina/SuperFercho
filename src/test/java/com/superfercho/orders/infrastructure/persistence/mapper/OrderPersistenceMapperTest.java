package com.superfercho.orders.infrastructure.persistence.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.superfercho.orders.domain.model.Order;
import com.superfercho.orders.domain.model.OrderItem;
import com.superfercho.orders.domain.model.OrderNumber;
import com.superfercho.orders.domain.model.OrderStatus;
import com.superfercho.orders.domain.model.ShippingAddressSnapshot;
import com.superfercho.orders.infrastructure.persistence.entity.OrderItemJpaEntity;
import com.superfercho.orders.infrastructure.persistence.entity.OrderJpaEntity;
import com.superfercho.platform.money.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrderPersistenceMapperTest {

    private static final Instant CREATED_AT = Instant.parse("2026-03-01T10:00:00Z");
    private static final Instant CONFIRMED_AT = Instant.parse("2026-03-01T10:16:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-03-01T10:20:00Z");
    private static final UUID ORDER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID CUSTOMER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID PRODUCT_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID SECOND_PRODUCT_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID PAYMENT_ID = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
    private static final UUID ITEM_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");

    private final OrderPersistenceMapper mapper = new OrderPersistenceMapper();

    @Test
    void shouldMapPendingOrderRoundTrip() {
        Order order = pendingOrder(null);

        Order mapped = mapper.toDomain(mapper.toEntity(order));

        assertEquals(order.id(), mapped.id());
        assertEquals(order.orderNumber().value(), mapped.orderNumber().value());
        assertEquals(order.customerId(), mapped.customerId());
        assertEquals(OrderStatus.PENDING, mapped.status());
        assertEquals(order.subtotal(), mapped.subtotal());
        assertEquals(order.total(), mapped.total());
        assertEquals(order.createdAt(), mapped.createdAt());
        assertEquals(order.updatedAt(), mapped.updatedAt());
        assertNull(mapped.confirmedAt());
        assertNull(mapped.cancelledAt());
        assertNull(mapped.paymentId());
    }

    @Test
    void shouldMapNullablePaymentId() {
        OrderJpaEntity entity = mapper.toEntity(pendingOrder(null));

        assertNull(entity.getPaymentId());
        assertNull(mapper.toDomain(entity).paymentId());
    }

    @Test
    void shouldMapPaymentIdWhenPresent() {
        Order order = pendingOrder(PAYMENT_ID);

        Order mapped = mapper.toDomain(mapper.toEntity(order));

        assertEquals(PAYMENT_ID, mapped.paymentId());
        assertEquals(PAYMENT_ID, mapper.toEntity(order).getPaymentId());
    }

    @Test
    void shouldMapItemsAndMoney() {
        Order order = pendingOrder(PAYMENT_ID);
        OrderJpaEntity entity = mapper.toEntity(order);

        assertEquals(1, entity.getItems().size());
        OrderItemJpaEntity item = entity.getItems().get(0);
        assertEquals(ITEM_ID, item.getId());
        assertEquals(PRODUCT_ID, item.getProductId());
        assertEquals("Leche entera", item.getProductName());
        assertEquals(new BigDecimal("10.50"), item.getUnitPriceAmount());
        assertEquals(Money.COP, item.getUnitPriceCurrency());
        assertEquals(2, item.getQuantity());
        assertEquals(new BigDecimal("21.00"), item.getSubtotalAmount());

        Order mapped = mapper.toDomain(entity);
        assertEquals(order.items(), mapped.items());
        assertEquals(Money.cop(new BigDecimal("21.00")), mapped.total());
    }

    @Test
    void shouldPreserveItemOrder() {
        Order order = Order.create(
                ORDER_ID,
                new OrderNumber("ORD-1002"),
                CUSTOMER_ID,
                List.of(milk(2), bread(1)),
                shippingAddress(),
                PAYMENT_ID,
                CREATED_AT,
                CREATED_AT);

        Order mapped = mapper.toDomain(mapper.toEntity(order));

        assertEquals("Leche entera", mapped.items().get(0).productName());
        assertEquals("Pan", mapped.items().get(1).productName());
        assertEquals(2, mapped.items().get(0).quantity());
        assertEquals(1, mapped.items().get(1).quantity());
    }

    @Test
    void shouldMapShippingAddressSnapshotIncludingNullAdditionalInfo() {
        Order order = pendingOrder(PAYMENT_ID);
        OrderJpaEntity entity = mapper.toEntity(order);

        assertEquals("Ada Lovelace", entity.getShippingRecipientName());
        assertEquals("Calle 1 # 2-3", entity.getShippingAddressLine());
        assertEquals("Apto 101", entity.getShippingAdditionalInfo());
        assertEquals("Bogotá", entity.getShippingCity());
        assertEquals("Cundinamarca", entity.getShippingDepartment());
        assertEquals("3001234567", entity.getShippingPhone());

        Order withoutExtra = Order.create(
                ORDER_ID,
                new OrderNumber("ORD-1001"),
                CUSTOMER_ID,
                List.of(milk(2)),
                new ShippingAddressSnapshot("Ada Lovelace", "Calle 1 # 2-3", null, "Bogotá", "Cundinamarca", "3001234567"),
                null,
                CREATED_AT,
                CREATED_AT);
        assertNull(mapper.toDomain(mapper.toEntity(withoutExtra)).shippingAddress().additionalInfo());
    }

    @Test
    void shouldPreservePersistedTimestampsAndAddressOnRoundTrip() {
        Order order = pendingOrder(PAYMENT_ID);
        Order mapped = mapper.toDomain(mapper.toEntity(order));

        assertEquals(order.id(), mapped.id());
        assertEquals(order.orderNumber().value(), mapped.orderNumber().value());
        assertEquals(order.customerId(), mapped.customerId());
        assertEquals(order.items(), mapped.items());
        assertEquals(order.shippingAddress(), mapped.shippingAddress());
        assertEquals(order.paymentId(), mapped.paymentId());
        assertEquals(order.status(), mapped.status());
        assertEquals(order.createdAt(), mapped.createdAt());
        assertEquals(order.updatedAt(), mapped.updatedAt());
        assertNull(mapped.confirmedAt());
        assertNull(mapped.cancelledAt());
    }

    @Test
    void shouldReconstituteConfirmedOrderWhenConfirmedAtDiffersFromUpdatedAt() {
        Order confirmed = Order.reconstitute(
                ORDER_ID,
                new OrderNumber("ORD-1001"),
                CUSTOMER_ID,
                OrderStatus.CONFIRMED,
                List.of(milk(2)),
                shippingAddress(),
                PAYMENT_ID,
                CREATED_AT,
                CONFIRMED_AT,
                null,
                UPDATED_AT);

        Order mapped = mapper.toDomain(mapper.toEntity(confirmed));

        assertEquals(OrderStatus.CONFIRMED, mapped.status());
        assertEquals(CREATED_AT, mapped.createdAt());
        assertEquals(CONFIRMED_AT, mapped.confirmedAt());
        assertEquals(UPDATED_AT, mapped.updatedAt());
        assertNull(mapped.cancelledAt());
        assertEquals(PAYMENT_ID, mapped.paymentId());
        assertEquals(confirmed.items(), mapped.items());
        assertEquals(confirmed.shippingAddress(), mapped.shippingAddress());
    }

    @Test
    void shouldReconstitutePreparingReadyAndDeliveredWithoutReplayingTransitions() {
        Order preparing = reconstituted(OrderStatus.PREPARING, CONFIRMED_AT, null, UPDATED_AT);
        Order ready = reconstituted(OrderStatus.READY, CONFIRMED_AT, null, UPDATED_AT);
        Order delivered = reconstituted(OrderStatus.DELIVERED, CONFIRMED_AT, null, UPDATED_AT);

        Order mappedPreparing = mapper.toDomain(mapper.toEntity(preparing));
        Order mappedReady = mapper.toDomain(mapper.toEntity(ready));
        Order mappedDelivered = mapper.toDomain(mapper.toEntity(delivered));

        assertEquals(OrderStatus.PREPARING, mappedPreparing.status());
        assertEquals(OrderStatus.READY, mappedReady.status());
        assertEquals(OrderStatus.DELIVERED, mappedDelivered.status());
        assertEquals(CONFIRMED_AT, mappedPreparing.confirmedAt());
        assertEquals(UPDATED_AT, mappedPreparing.updatedAt());
        assertEquals(CONFIRMED_AT, mappedReady.confirmedAt());
        assertEquals(UPDATED_AT, mappedReady.updatedAt());
        assertEquals(CONFIRMED_AT, mappedDelivered.confirmedAt());
        assertEquals(UPDATED_AT, mappedDelivered.updatedAt());
        assertEquals(CREATED_AT, mappedDelivered.createdAt());
        assertNull(mappedDelivered.cancelledAt());
        assertEquals(PAYMENT_ID, mappedDelivered.paymentId());
    }

    @Test
    void shouldMapCancelledStatusAndTimestamps() {
        Instant cancelledAt = Instant.parse("2026-03-01T10:10:00Z");
        Order cancelled = reconstituted(OrderStatus.CANCELLED, null, cancelledAt, cancelledAt);

        Order mapped = mapper.toDomain(mapper.toEntity(cancelled));

        assertEquals(OrderStatus.CANCELLED, mapped.status());
        assertEquals(cancelledAt, mapped.cancelledAt());
        assertEquals(cancelledAt, mapped.updatedAt());
        assertEquals(CREATED_AT, mapped.createdAt());
        assertNull(mapped.confirmedAt());
        assertEquals(PAYMENT_ID, mapped.paymentId());
        assertEquals(cancelled.items(), mapped.items());
        assertEquals(cancelled.shippingAddress(), mapped.shippingAddress());
    }

    private static Order reconstituted(
            OrderStatus status, Instant confirmedAt, Instant cancelledAt, Instant updatedAt) {
        return Order.reconstitute(
                ORDER_ID,
                new OrderNumber("ORD-1001"),
                CUSTOMER_ID,
                status,
                List.of(milk(2)),
                shippingAddress(),
                PAYMENT_ID,
                CREATED_AT,
                confirmedAt,
                cancelledAt,
                updatedAt);
    }

    private static Order pendingOrder(UUID paymentId) {
        return Order.create(
                ORDER_ID,
                new OrderNumber("ORD-1001"),
                CUSTOMER_ID,
                List.of(milk(2)),
                shippingAddress(),
                paymentId,
                CREATED_AT,
                CREATED_AT);
    }

    private static OrderItem milk(int quantity) {
        return OrderItem.create(ITEM_ID, PRODUCT_ID, "Leche entera", Money.cop(new BigDecimal("10.50")), quantity);
    }

    private static OrderItem bread(int quantity) {
        return OrderItem.create(
                UUID.fromString("88888888-8888-8888-8888-888888888888"),
                SECOND_PRODUCT_ID,
                "Pan",
                Money.cop(new BigDecimal("3.00")),
                quantity);
    }

    private static ShippingAddressSnapshot shippingAddress() {
        return new ShippingAddressSnapshot(
                "Ada Lovelace", "Calle 1 # 2-3", "Apto 101", "Bogotá", "Cundinamarca", "3001234567");
    }
}
