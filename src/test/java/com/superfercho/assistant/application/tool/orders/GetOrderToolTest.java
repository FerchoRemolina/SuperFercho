package com.superfercho.assistant.application.tool.orders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.superfercho.assistant.application.tool.ToolArguments;
import com.superfercho.orders.application.dto.GetOrderCommand;
import com.superfercho.orders.application.dto.OrderResult;
import com.superfercho.orders.application.dto.ShippingAddressResult;
import com.superfercho.orders.application.usecase.GetOrderUseCase;
import com.superfercho.orders.domain.model.OrderStatus;
import com.superfercho.platform.money.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetOrderToolTest {

    private static final UUID ORDER_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    @Mock
    private GetOrderUseCase getOrderUseCase;

    @Test
    void delegatesToGetOrderUseCase() {
        Instant now = Instant.parse("2026-04-01T10:00:00Z");
        when(getOrderUseCase.execute(new GetOrderCommand(ORDER_ID)))
                .thenReturn(new OrderResult(
                        ORDER_ID,
                        "SF-1",
                        OrderStatus.PENDING,
                        List.of(),
                        Money.cop(new BigDecimal("10.50")),
                        Money.cop(new BigDecimal("10.50")),
                        new ShippingAddressResult("Ada", "Calle 1", null, "Bogotá", "Cundinamarca", "300"),
                        UUID.randomUUID(),
                        now,
                        null,
                        null,
                        now));

        assertEquals(
                true,
                new GetOrderTool(getOrderUseCase)
                        .execute(ToolArguments.of(Map.of("orderId", ORDER_ID.toString())))
                        .success());
        verify(getOrderUseCase).execute(new GetOrderCommand(ORDER_ID));
    }
}
