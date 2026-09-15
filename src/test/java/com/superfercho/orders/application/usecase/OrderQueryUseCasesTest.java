package com.superfercho.orders.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.superfercho.orders.application.dto.GetOrderCommand;
import com.superfercho.orders.application.dto.ListOrdersCommand;
import com.superfercho.orders.application.dto.OrderResult;
import com.superfercho.orders.application.dto.PageRequest;
import com.superfercho.orders.application.dto.PagedResult;
import com.superfercho.orders.application.exception.OrderOwnershipException;
import com.superfercho.orders.application.port.CurrentUserProvider;
import com.superfercho.orders.application.port.OrderRepository;
import com.superfercho.orders.domain.model.Order;
import com.superfercho.orders.domain.model.OrderItem;
import com.superfercho.orders.domain.model.OrderNumber;
import com.superfercho.orders.domain.model.ShippingAddressSnapshot;
import com.superfercho.platform.money.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderQueryUseCasesTest {

    private static final Instant CREATED_AT = Instant.parse("2026-03-01T10:00:00Z");
    private static final UUID CUSTOMER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_CUSTOMER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ORDER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID PRODUCT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private OrderRepository orderRepository;

    private GetOrderUseCase getOrder;
    private ListOrdersUseCase listOrders;

    @BeforeEach
    void setUp() {
        getOrder = new GetOrderUseCase(currentUserProvider, orderRepository);
        listOrders = new ListOrdersUseCase(currentUserProvider, orderRepository);
        when(currentUserProvider.getCurrentUserId()).thenReturn(CUSTOMER_ID);
    }

    @Test
    void shouldReturnOwnedOrder() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(pendingOrder(CUSTOMER_ID)));

        OrderResult result = getOrder.execute(new GetOrderCommand(ORDER_ID));

        assertEquals(ORDER_ID, result.id());
        assertEquals("ORD-1001", result.orderNumber());
        assertEquals(CUSTOMER_ID, pendingOrder(CUSTOMER_ID).customerId());
        assertEquals(1, result.items().size());
        assertEquals("Leche entera", result.items().get(0).productName());
    }

    @Test
    void shouldRejectAccessToAnotherCustomersOrder() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(pendingOrder(OTHER_CUSTOMER_ID)));

        assertThrows(OrderOwnershipException.class, () -> getOrder.execute(new GetOrderCommand(ORDER_ID)));
    }

    @Test
    void shouldListOnlyCurrentCustomerOrdersWithPaginationDefaults() {
        Order order = pendingOrder(CUSTOMER_ID);
        when(orderRepository.findByCustomerId(CUSTOMER_ID, PageRequest.of(null, null)))
                .thenReturn(new PagedResult<>(List.of(order), 0, 20, 1));

        PagedResult<OrderResult> result = listOrders.execute(new ListOrdersCommand(null, null));

        assertEquals(1, result.items().size());
        assertEquals(ORDER_ID, result.items().get(0).id());
        assertEquals(0, result.page());
        assertEquals(20, result.size());
        verify(orderRepository).findByCustomerId(CUSTOMER_ID, new PageRequest(0, 20));
    }

    @Test
    void shouldCapPageSizeAtMaximum() {
        when(orderRepository.findByCustomerId(CUSTOMER_ID, PageRequest.of(2, 250)))
                .thenReturn(new PagedResult<>(List.of(), 2, 100, 0));

        PagedResult<OrderResult> result = listOrders.execute(new ListOrdersCommand(2, 250));

        assertEquals(2, result.page());
        assertEquals(100, result.size());
        verify(orderRepository).findByCustomerId(CUSTOMER_ID, new PageRequest(2, 100));
    }

    private static Order pendingOrder(UUID customerId) {
        return Order.create(
                ORDER_ID,
                new OrderNumber("ORD-1001"),
                customerId,
                List.of(OrderItem.create(
                        UUID.fromString("99999999-9999-9999-9999-999999999999"),
                        PRODUCT_ID,
                        "Leche entera",
                        Money.cop(new BigDecimal("10.50")),
                        2)),
                new ShippingAddressSnapshot(
                        "Ada Lovelace", "Calle 1 # 2-3", "Apto 101", "Bogotá", "Cundinamarca", "3001234567"),
                null,
                CREATED_AT,
                CREATED_AT);
    }
}
