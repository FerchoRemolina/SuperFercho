package com.superfercho.assistant.application.tool.orders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.superfercho.assistant.application.port.out.ClockPort;
import com.superfercho.assistant.application.port.out.CurrentUserProvider;
import com.superfercho.assistant.application.tool.ToolArguments;
import com.superfercho.assistant.application.tool.ToolResult;
import com.superfercho.assistant.infrastructure.confirmation.InMemoryPendingSensitiveActionStore;
import com.superfercho.orders.application.dto.GetOrderCommand;
import com.superfercho.orders.application.dto.OrderResult;
import com.superfercho.orders.application.dto.ShippingAddressResult;
import com.superfercho.orders.application.exception.OrderNotFoundException;
import com.superfercho.orders.application.exception.OrderOwnershipException;
import com.superfercho.orders.application.usecase.GetOrderUseCase;
import com.superfercho.orders.domain.model.OrderStatus;
import com.superfercho.platform.money.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CancelOrderToolTest {

    private static final Instant NOW = Instant.parse("2026-04-01T10:00:00Z");
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ORDER_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private GetOrderUseCase getOrderUseCase;

    @Mock
    private ClockPort clockPort;

    private InMemoryPendingSensitiveActionStore pending;
    private CancelOrderTool tool;

    @BeforeEach
    void setUp() {
        pending = new InMemoryPendingSensitiveActionStore();
        tool = new CancelOrderTool(currentUserProvider, getOrderUseCase, clockPort, pending);
        when(currentUserProvider.getCurrentUserId()).thenReturn(USER_ID);
    }

    @Test
    void preparesConfirmationForOwnedCancellableOrderWithoutCancelling() {
        when(clockPort.currentTime()).thenReturn(NOW);
        when(getOrderUseCase.execute(new GetOrderCommand(ORDER_ID)))
                .thenReturn(order(OrderStatus.CONFIRMED, NOW));

        ToolResult result = tool.execute(ToolArguments.of(Map.of("orderId", ORDER_ID.toString())));

        assertTrue(result.success());
        assertTrue(result.awaitsConfirmation());
        assertTrue(pending.findByToken(result.confirmationToken()).isPresent());
        verify(getOrderUseCase).execute(new GetOrderCommand(ORDER_ID));
        assertEquals(USER_ID, pending.findByUserId(USER_ID).orElseThrow().userId());
    }

    @Test
    void rejectsMissingOrderWithoutCreatingConfirmation() {
        when(getOrderUseCase.execute(new GetOrderCommand(ORDER_ID))).thenThrow(new OrderNotFoundException(ORDER_ID));

        ToolResult result = tool.execute(ToolArguments.of(Map.of("orderId", ORDER_ID.toString())));

        assertFailureWithoutConfirmation(result, "order not found");
        verify(clockPort, never()).currentTime();
    }

    @Test
    void rejectsForeignOrderWithoutRevealingOwnership() {
        when(getOrderUseCase.execute(new GetOrderCommand(ORDER_ID)))
                .thenThrow(new OrderOwnershipException(OTHER_USER_ID, ORDER_ID));

        ToolResult result = tool.execute(ToolArguments.of(Map.of("orderId", ORDER_ID.toString())));

        assertFailureWithoutConfirmation(result, "order not found");
    }

    @Test
    void rejectsNonCancellableOrderWithoutCreatingConfirmation() {
        when(clockPort.currentTime()).thenReturn(NOW);
        when(getOrderUseCase.execute(new GetOrderCommand(ORDER_ID)))
                .thenReturn(order(OrderStatus.PREPARING, NOW));

        ToolResult result = tool.execute(ToolArguments.of(Map.of("orderId", ORDER_ID.toString())));

        assertFailureWithoutConfirmation(result, "order cannot be cancelled in its current state");
    }

    @Test
    void rejectsExpiredCancellationWindowWithoutCreatingConfirmation() {
        when(clockPort.currentTime()).thenReturn(NOW);
        when(getOrderUseCase.execute(new GetOrderCommand(ORDER_ID)))
                .thenReturn(order(OrderStatus.CONFIRMED, NOW.minusSeconds(121)));

        ToolResult result = tool.execute(ToolArguments.of(Map.of("orderId", ORDER_ID.toString())));

        assertFailureWithoutConfirmation(result, "order cannot be cancelled in its current state");
    }

    private void assertFailureWithoutConfirmation(ToolResult result, String message) {
        assertFalse(result.success());
        assertFalse(result.awaitsConfirmation());
        assertEquals(message, result.content());
        assertTrue(pending.findByUserId(USER_ID).isEmpty());
    }

    private static OrderResult order(OrderStatus status, Instant confirmedAt) {
        return new OrderResult(
                ORDER_ID,
                "SF-1",
                USER_ID,
                status,
                List.of(),
                Money.cop(new BigDecimal("10.50")),
                Money.cop(new BigDecimal("10.50")),
                new ShippingAddressResult("Ada", "Calle 1", null, "Bogotá", "Cundinamarca", "300"),
                UUID.randomUUID(),
                NOW,
                confirmedAt,
                null,
                NOW,
                null);
    }
}
