package com.superfercho.orders.application.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.superfercho.orders.domain.exception.InvalidOrderException;
import com.superfercho.orders.domain.model.OrderStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ListAdminOrdersCommandTest {

    @Test
    void shouldApplyDefaultsAndNormalizeStatuses() {
        ListAdminOrdersCommand command =
                ListAdminOrdersCommand.of(null, null, List.of("CONFIRMED,DELIVERED", " ", "CONFIRMED"));

        assertNull(command.page());
        assertNull(command.size());
        assertEquals(List.of(OrderStatus.CONFIRMED, OrderStatus.DELIVERED), command.statuses());
        assertTrue(command.hasStatusFilter());
        assertNull(command.orderNumber());
        assertNull(command.fromInclusive());
        assertNull(command.toExclusive());
    }

    @Test
    void shouldTreatBlankOrderNumberAsAbsent() {
        ListAdminOrdersCommand command = ListAdminOrdersCommand.of(0, 20, null, "   ", null, null);

        assertNull(command.orderNumber());
    }

    @Test
    void shouldTrimOrderNumber() {
        ListAdminOrdersCommand command = ListAdminOrdersCommand.of(0, 20, null, "  ORD-ABC123  ", null, null);

        assertEquals("ORD-ABC123", command.orderNumber());
    }

    @Test
    void shouldParseBareDatesInBogota() {
        ListAdminOrdersCommand command =
                ListAdminOrdersCommand.of(null, null, null, null, "2026-03-01", "2026-04-01");

        assertEquals(Instant.parse("2026-03-01T05:00:00Z"), command.fromInclusive());
        assertEquals(Instant.parse("2026-04-01T05:00:00Z"), command.toExclusive());
    }

    @Test
    void shouldParseIsoInstantsAsGiven() {
        ListAdminOrdersCommand command = ListAdminOrdersCommand.of(
                null, null, null, null, "2026-03-01T00:00:00-05:00", "2026-03-02T00:00:00Z");

        assertEquals(Instant.parse("2026-03-01T05:00:00Z"), command.fromInclusive());
        assertEquals(Instant.parse("2026-03-02T00:00:00Z"), command.toExclusive());
    }

    @Test
    void shouldRejectInvalidDateBound() {
        InvalidOrderException exception = assertThrows(
                InvalidOrderException.class,
                () -> ListAdminOrdersCommand.of(null, null, null, null, "01/03/2026", null));

        assertTrue(exception.getMessage().contains("from"));
    }

    @Test
    void shouldRejectRangeWithFromAfterTo() {
        assertThrows(
                InvalidOrderException.class,
                () -> ListAdminOrdersCommand.of(
                        null, null, null, null, "2026-04-01", "2026-03-01"));
    }

    @Test
    void shouldRejectRangeWithEqualBounds() {
        assertThrows(
                InvalidOrderException.class,
                () -> ListAdminOrdersCommand.of(
                        null, null, null, null, "2026-03-01", "2026-03-01"));
    }

    @Test
    void shouldAcceptFromOnlyAndToOnlyRanges() {
        ListAdminOrdersCommand fromOnly =
                ListAdminOrdersCommand.of(null, null, null, null, "2026-03-01", null);
        ListAdminOrdersCommand toOnly =
                ListAdminOrdersCommand.of(null, null, null, null, null, "2026-04-01");

        assertEquals(Instant.parse("2026-03-01T05:00:00Z"), fromOnly.fromInclusive());
        assertNull(fromOnly.toExclusive());
        assertNull(toOnly.fromInclusive());
        assertEquals(Instant.parse("2026-04-01T05:00:00Z"), toOnly.toExclusive());
    }

    @Test
    void shouldRejectInvalidStatusFilter() {
        assertThrows(
                InvalidOrderException.class,
                () -> ListAdminOrdersCommand.of(0, 20, List.of("SOLD")));
    }

    @Test
    void shouldKeepEmptyStatusListWhenNoFilterGiven() {
        ListAdminOrdersCommand command = ListAdminOrdersCommand.of(null, null, null);

        assertEquals(List.of(), command.statuses());
        assertEquals(false, command.hasStatusFilter());
    }
}
