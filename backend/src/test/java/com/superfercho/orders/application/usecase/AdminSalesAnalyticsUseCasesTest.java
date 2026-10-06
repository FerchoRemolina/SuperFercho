package com.superfercho.orders.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.superfercho.orders.application.dto.AdminBusinessPeriodRow;
import com.superfercho.orders.application.dto.AdminCustomerSalesRow;
import com.superfercho.orders.application.dto.AdminOrderPeriodSummaryResult;
import com.superfercho.orders.application.dto.AdminProductSalesRow;
import com.superfercho.orders.application.dto.AdminSalesBucketRow;
import com.superfercho.orders.application.dto.GetAdminDashboardSummaryCommand;
import com.superfercho.orders.application.dto.GetAdminSalesPeriodAnalyticsCommand;
import com.superfercho.orders.application.dto.GetAdminTopCustomersCommand;
import com.superfercho.orders.application.dto.GetAdminTopProductsCommand;
import com.superfercho.orders.domain.exception.InvalidSalesPeriodException;
import com.superfercho.orders.domain.model.OrderStatus;
import com.superfercho.platform.money.Money;
import com.superfercho.platform.time.BucketGranularity;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminSalesAnalyticsUseCasesTest {

    private static final ZoneId BOGOTA = com.superfercho.platform.time.BusinessZone.BOGOTA;
    private static final Instant MAY_START = Instant.parse("2026-05-01T05:00:00Z");
    private static final Instant JUNE_START = Instant.parse("2026-06-01T05:00:00Z");

    @Mock
    private com.superfercho.orders.application.port.OrderRepository orderRepository;

    // ---------------------------------------------------------------- sales

    @Test
    void shouldGroupSalesByBogotaDayAndFillEmptyBuckets() {
        Instant from = MAY_START;
        Instant to = Instant.parse("2026-05-04T05:00:00Z");
        when(orderRepository.aggregateSalesBuckets(BucketGranularity.DAY, from, to))
                .thenReturn(List.of(
                        new AdminSalesBucketRow(MAY_START, new BigDecimal("125.00"), 8),
                        new AdminSalesBucketRow(
                                Instant.parse("2026-05-03T05:00:00Z"), new BigDecimal("40.00"), 2)));

        var result = new GetAdminSalesPeriodAnalyticsUseCase(orderRepository)
                .execute(GetAdminSalesPeriodAnalyticsCommand.of(
                        "2026-05-01T00:00:00", "2026-05-04T00:00:00", "DAY"));

        assertEquals(BucketGranularity.DAY, result.granularity());
        assertEquals(3, result.buckets().size());
        assertEquals(MAY_START, result.buckets().get(0).periodStart());
        assertEquals(8, result.buckets().get(0).orderCount());
        assertEquals(0, new BigDecimal("125.00").compareTo(result.buckets().get(0).total().amount()));
        assertEquals(0, result.buckets().get(1).orderCount());
        assertEquals(0, BigDecimal.ZERO.compareTo(result.buckets().get(1).total().amount()));
        assertEquals(2, result.buckets().get(2).orderCount());
        verify(orderRepository).aggregateSalesBuckets(BucketGranularity.DAY, from, to);
    }

    @Test
    void shouldBucketLateNightUtcOrderIntoPreviousBogotaDay() {
        // 2026-05-01T02:00:00Z == 2026-04-30 21:00 Bogota
        GetAdminSalesPeriodAnalyticsCommand command = GetAdminSalesPeriodAnalyticsCommand.of(
                "2026-04-30T00:00:00", "2026-05-02T00:00:00", "DAY");
        Instant april30Start = Instant.parse("2026-04-30T05:00:00Z");
        Instant may1Start = Instant.parse("2026-05-01T05:00:00Z");
        when(orderRepository.aggregateSalesBuckets(BucketGranularity.DAY, command.from(), command.to()))
                .thenReturn(List.of(new AdminSalesBucketRow(april30Start, new BigDecimal("10.00"), 1)));

        var result = new GetAdminSalesPeriodAnalyticsUseCase(orderRepository).execute(command);

        assertEquals(april30Start, result.buckets().get(0).periodStart());
        assertEquals(may1Start, result.buckets().get(1).periodStart());
        assertEquals(LocalDate.ofInstant(april30Start, BOGOTA), LocalDate.of(2026, 4, 30));
    }

    @Test
    void shouldGroupSalesByMonthBuckets() {
        GetAdminSalesPeriodAnalyticsCommand command = GetAdminSalesPeriodAnalyticsCommand.of(
                "2026-01-01T00:00:00", "2026-04-01T00:00:00", "MONTH");
        Instant january = YearMonth.of(2026, 1).atDay(1).atStartOfDay(BOGOTA).toInstant();
        Instant march = YearMonth.of(2026, 3).atDay(1).atStartOfDay(BOGOTA).toInstant();
        when(orderRepository.aggregateSalesBuckets(BucketGranularity.MONTH, command.from(), command.to()))
                .thenReturn(List.of(new AdminSalesBucketRow(january, new BigDecimal("90.00"), 3)));

        var result = new GetAdminSalesPeriodAnalyticsUseCase(orderRepository).execute(command);

        assertEquals(3, result.buckets().size());
        assertEquals(january, result.buckets().get(0).periodStart());
        assertEquals(3, result.buckets().get(0).orderCount());
        assertEquals(0, result.buckets().get(1).orderCount());
        assertEquals(march, result.buckets().get(2).periodStart());
    }

    @Test
    void shouldSupportFullHistoricMonthWithHourlyBuckets() {
        GetAdminSalesPeriodAnalyticsCommand command =
                GetAdminSalesPeriodAnalyticsCommand.of("2026-05-01T05:00:00Z", "2026-05-02T05:00:00Z", "HOUR");

        var result = new GetAdminSalesPeriodAnalyticsUseCase(orderRepository).execute(command);

        assertEquals(24, result.buckets().size());
        assertEquals(MAY_START, result.buckets().get(0).periodStart());
    }

    // -------------------------------------------------------- command rules

    @Test
    void shouldInterpretNaiveDateTimesAsBogotaLocalTime() {
        GetAdminSalesPeriodAnalyticsCommand command =
                GetAdminSalesPeriodAnalyticsCommand.of("2026-05-01T00:00:00", "2026-06-01T00:00:00", "DAY");

        assertEquals(MAY_START, command.from());
        assertEquals(JUNE_START, command.to());
    }

    @Test
    void shouldInterpretBareDatesAsBogotaMidnight() {
        GetAdminSalesPeriodAnalyticsCommand command =
                GetAdminSalesPeriodAnalyticsCommand.of("2026-05-01", "2026-06-01", null);

        assertEquals(MAY_START, command.from());
        assertEquals(JUNE_START, command.to());
        assertEquals(BucketGranularity.DAY, command.granularity());
    }

    @Test
    void shouldHonorExplicitOffsets() {
        GetAdminSalesPeriodAnalyticsCommand command = GetAdminSalesPeriodAnalyticsCommand.of(
                "2026-05-01T00:00:00-05:00", "2026-06-01T00:00:00-05:00", "MONTH");

        assertEquals(MAY_START, command.from());
        assertEquals(JUNE_START, command.to());
    }

    @Test
    void shouldRejectMissingOrSwappedBoundsAndOversizedRanges() {
        assertThrows(InvalidSalesPeriodException.class, () -> GetAdminSalesPeriodAnalyticsCommand.of(
                null, "2026-06-01", "DAY"));
        assertThrows(InvalidSalesPeriodException.class, () -> GetAdminSalesPeriodAnalyticsCommand.of(
                "2026-06-01", null, "DAY"));
        assertThrows(InvalidSalesPeriodException.class, () -> GetAdminSalesPeriodAnalyticsCommand.of(
                "2026-06-01", "2026-05-01", "DAY"));
        assertThrows(InvalidSalesPeriodException.class, () -> GetAdminSalesPeriodAnalyticsCommand.of(
                "2026-05-01", "2026-05-01", "DAY"));
        assertThrows(InvalidSalesPeriodException.class, () -> GetAdminSalesPeriodAnalyticsCommand.of(
                "2020-01-01", "2026-01-01", "DAY"));
        assertThrows(InvalidSalesPeriodException.class, () -> GetAdminSalesPeriodAnalyticsCommand.of(
                "2026-05-01", "2026-06-01", "QUARTER"));
        assertThrows(InvalidSalesPeriodException.class, () -> GetAdminSalesPeriodAnalyticsCommand.of(
                "not-a-date", "2026-06-01", "DAY"));
    }

    @Test
    void shouldLimitHourRangeToSevenDays() {
        assertThrows(InvalidSalesPeriodException.class, () -> GetAdminSalesPeriodAnalyticsCommand.of(
                "2026-05-01T00:00:00", "2026-05-09T00:00:00", "HOUR"));
        assertTrue(GetAdminSalesPeriodAnalyticsCommand.of("2026-05-01T00:00:00", "2026-05-08T00:00:00", "HOUR")
                != null);
    }

    @Test
    void shouldLimitDayRangeToFourHundredDays() {
        assertThrows(InvalidSalesPeriodException.class, () -> GetAdminSalesPeriodAnalyticsCommand.of(
                "2025-01-01", "2026-06-01", "DAY"));
        assertTrue(GetAdminSalesPeriodAnalyticsCommand.of("2025-04-27", "2026-06-01", "DAY") != null);
    }

    // -------------------------------------------------------------- summary

    @Test
    void shouldSummarizeCountersByDeliveryAndCancellationTimestamps() {
        Instant from = MAY_START;
        Instant to = JUNE_START;
        when(orderRepository.summarizeBusinessPeriod(from, to))
                .thenReturn(new AdminBusinessPeriodRow(35, 4, new BigDecimal("350.00"), 10));

        AdminOrderPeriodSummaryResult result = new GetAdminOrderPeriodSummaryUseCase(orderRepository)
                .execute(new GetAdminDashboardSummaryCommand(from, to));

        assertEquals(10, result.inProcessOrders());
        assertEquals(35, result.deliveredOrders());
        assertEquals(4, result.cancelledOrders());
        assertEquals(39, result.totalOrders());
        assertEquals(0, new BigDecimal("350.00").compareTo(result.sales().amount()));
        assertEquals("COP", result.sales().currency());
    }

    @Test
    void shouldRejectInvalidSummaryPeriod() {
        assertThrows(
                InvalidSalesPeriodException.class, () -> GetAdminDashboardSummaryCommand.of("2026-06-01", "2026-05-01"));
        assertThrows(InvalidSalesPeriodException.class, () -> GetAdminDashboardSummaryCommand.of(null, "2026-05-01"));
    }

    // ------------------------------------------------------------ rankings

    @Test
    void shouldRankProductsByUnitsWithSortAndLimit() {
        GetAdminTopProductsCommand desc =
                GetAdminTopProductsCommand.of("2026-05-01", "2026-06-01", 3, "DESC");
        when(orderRepository.findTopProductsByQuantity(desc.from(), desc.to(), 3, false))
                .thenReturn(List.of(
                        new AdminProductSalesRow(UUID.fromString("33333333-3333-3333-3333-333333333333"),
                                "Leche entera", 25)));

        var result = new GetAdminTopProductsUseCase(orderRepository).execute(desc);

        assertEquals(1, result.size());
        assertEquals(25, result.get(0).quantity());
        assertEquals("Leche entera", result.get(0).productName());
        verify(orderRepository).findTopProductsByQuantity(desc.from(), desc.to(), 3, false);

        GetAdminTopProductsCommand asc = GetAdminTopProductsCommand.of("2026-05-01", "2026-06-01", null, "asc");
        assertEquals(5, asc.limit());
        assertTrue(asc.ascending());
    }

    @Test
    void shouldDefaultProductSortToDescAndClampLimit() {
        GetAdminTopProductsCommand command =
                GetAdminTopProductsCommand.of("2026-05-01", "2026-06-01", 500, null);

        assertFalse(command.ascending());
        assertEquals(50, command.limit());
        assertThrows(InvalidSalesPeriodException.class, () -> GetAdminTopProductsCommand.of(
                "2026-05-01", "2026-06-01", 5, "SIDEWAYS"));
    }

    @Test
    void shouldRankCustomersByTotalPurchased() {
        GetAdminTopCustomersCommand command = GetAdminTopCustomersCommand.of("2026-05-01", "2026-06-01", null);
        UUID customerId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        when(orderRepository.findTopCustomersByTotal(command.from(), command.to(), 5))
                .thenReturn(List.of(new AdminCustomerSalesRow(
                        customerId, "Ada Lovelace", new BigDecimal("450000.00"), 6)));

        var result = new GetAdminTopCustomersUseCase(orderRepository).execute(command);

        assertEquals(1, result.size());
        assertEquals(customerId, result.get(0).customerId());
        assertEquals("Ada Lovelace", result.get(0).customerName());
        assertEquals(0, new BigDecimal("450000.00").compareTo(result.get(0).totalAmount()));
        assertEquals(6, result.get(0).orderCount());
        assertEquals(5, command.limit());
        assertEquals(GetAdminTopCustomersCommand.SortBy.TOTAL, command.sortBy());
    }

    @Test
    void shouldRankCustomersByOrderCountWhenRequested() {
        GetAdminTopCustomersCommand command =
                GetAdminTopCustomersCommand.of("2026-05-01", "2026-06-01", 3, "ORDERS");
        UUID customerId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        when(orderRepository.findTopCustomersByOrders(command.from(), command.to(), 3))
                .thenReturn(List.of(new AdminCustomerSalesRow(
                        customerId, "Ada Lovelace", new BigDecimal("120000.00"), 9)));

        var result = new GetAdminTopCustomersUseCase(orderRepository).execute(command);

        assertEquals(1, result.size());
        assertEquals(9, result.get(0).orderCount());
        assertEquals(GetAdminTopCustomersCommand.SortBy.ORDERS, command.sortBy());
        verify(orderRepository).findTopCustomersByOrders(command.from(), command.to(), 3);
        verify(orderRepository, never()).findTopCustomersByTotal(any(), any(), anyInt());
    }
}
