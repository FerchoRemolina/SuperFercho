package com.superfercho.orders.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.superfercho.orders.application.dto.AdminRecentBuyerResult;
import com.superfercho.orders.application.dto.AdminSalesPeriodSummaryResult;
import com.superfercho.orders.application.dto.GetAdminSalesPeriodSummaryCommand;
import com.superfercho.orders.application.dto.ListAdminRecentBuyersCommand;
import com.superfercho.orders.application.port.ClockProvider;
import com.superfercho.orders.application.port.OrderRepository;
import com.superfercho.orders.domain.model.Order;
import com.superfercho.orders.domain.model.OrderItem;
import com.superfercho.orders.domain.model.OrderNumber;
import com.superfercho.orders.domain.model.OrderStatus;
import com.superfercho.orders.domain.model.SalesPeriodGranularity;
import com.superfercho.orders.domain.model.ShippingAddressSnapshot;
import com.superfercho.platform.money.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminDashboardUseCasesTest {

    private static final Instant NOW = Instant.parse("2026-03-15T15:30:00Z");
    private static final UUID CUSTOMER_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CUSTOMER_B = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PRODUCT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final Money TEN = Money.cop(new BigDecimal("10.00"));
    private static final Money TWENTY = Money.cop(new BigDecimal("20.00"));

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ClockProvider clockProvider;

    private GetAdminSalesPeriodSummaryUseCase salesUseCase;
    private ListAdminRecentBuyersUseCase buyersUseCase;

    @BeforeEach
    void setUp() {
        salesUseCase = new GetAdminSalesPeriodSummaryUseCase(orderRepository, clockProvider);
        buyersUseCase = new ListAdminRecentBuyersUseCase(orderRepository, clockProvider);
        when(clockProvider.currentTime()).thenReturn(NOW);
    }

    @Test
    void shouldBucketWeekSalesAcrossFullPeriodNotAPageSample() {
        Order older = order(
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                "ORD-1",
                CUSTOMER_A,
                Instant.parse("2026-03-10T12:00:00Z"),
                TEN,
                "Ada");
        Order newer = order(
                UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
                "ORD-2",
                CUSTOMER_B,
                Instant.parse("2026-03-14T09:00:00Z"),
                TWENTY,
                "Bob");
        when(orderRepository.findCreatedBetweenExcludingStatus(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        eq(OrderStatus.CANCELLED)))
                .thenReturn(List.of(older, newer));

        AdminSalesPeriodSummaryResult result =
                salesUseCase.execute(new GetAdminSalesPeriodSummaryCommand(SalesPeriodGranularity.WEEK));

        assertEquals(SalesPeriodGranularity.WEEK, result.granularity());
        assertEquals(7, result.buckets().size());
        long totalOrders = result.buckets().stream().mapToLong(b -> b.orderCount()).sum();
        assertEquals(2, totalOrders);
        BigDecimal sum = result.buckets().stream()
                .map(b -> b.total().amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, new BigDecimal("30.00").compareTo(sum));

        ArgumentCaptor<Instant> from = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> to = ArgumentCaptor.forClass(Instant.class);
        verify(orderRepository)
                .findCreatedBetweenExcludingStatus(from.capture(), to.capture(), eq(OrderStatus.CANCELLED));
        assertTrue(from.getValue().isBefore(to.getValue()));
        assertTrue(!from.getValue().isAfter(older.createdAt()));
        assertTrue(to.getValue().isAfter(newer.createdAt()));
    }

    @Test
    void shouldAggregateRecentBuyersByCustomerOrderedByLastPurchase() {
        Order firstA = order(
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                "ORD-1",
                CUSTOMER_A,
                Instant.parse("2026-03-01T10:00:00Z"),
                TEN,
                "Ada Lovelace");
        Order lastA = order(
                UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
                "ORD-2",
                CUSTOMER_A,
                Instant.parse("2026-03-14T10:00:00Z"),
                TWENTY,
                "Ada Lovelace");
        Order onlyB = order(
                UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"),
                "ORD-3",
                CUSTOMER_B,
                Instant.parse("2026-03-13T10:00:00Z"),
                TEN,
                "Bob Builder");
        when(orderRepository.findCreatedBetweenExcludingStatus(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        eq(OrderStatus.CANCELLED)))
                .thenReturn(List.of(firstA, lastA, onlyB));

        List<AdminRecentBuyerResult> buyers =
                buyersUseCase.execute(ListAdminRecentBuyersCommand.of(8));

        assertEquals(2, buyers.size());
        assertEquals(CUSTOMER_A, buyers.get(0).customerId());
        assertEquals("Ada Lovelace", buyers.get(0).displayName());
        assertEquals(2, buyers.get(0).orderCount());
        assertEquals(TWENTY, buyers.get(0).lastOrderTotal());
        assertEquals(Instant.parse("2026-03-14T10:00:00Z"), buyers.get(0).lastOrderAt());
        assertEquals(CUSTOMER_B, buyers.get(1).customerId());
        assertEquals(1, buyers.get(1).orderCount());
    }

    private static Order order(
            UUID id, String number, UUID customerId, Instant createdAt, Money total, String recipient) {
        int quantity = total.amount().intValue() / 10;
        return Order.create(
                id,
                new OrderNumber(number),
                customerId,
                List.of(OrderItem.create(
                        UUID.randomUUID(),
                        PRODUCT_ID,
                        "Producto",
                        Money.cop(new BigDecimal("10.00")),
                        Math.max(quantity, 1))),
                new ShippingAddressSnapshot(
                        recipient, "Calle 1", null, "Bogotá", "Cundinamarca", "3001234567"),
                null,
                createdAt,
                createdAt);
    }
}
