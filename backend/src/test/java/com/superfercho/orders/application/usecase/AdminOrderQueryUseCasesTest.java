package com.superfercho.orders.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.superfercho.orders.application.dto.AdminOrderFilter;
import com.superfercho.orders.application.dto.AdminOrderDetailResult;
import com.superfercho.orders.application.dto.AdminOrderListItemResult;
import com.superfercho.orders.application.dto.CustomerDirectoryEntry;
import com.superfercho.orders.application.dto.GetOrderCommand;
import com.superfercho.orders.application.dto.ListAdminOrdersCommand;
import com.superfercho.orders.application.dto.OrderResult;
import com.superfercho.orders.application.dto.PageRequest;
import com.superfercho.orders.application.dto.PagedResult;
import com.superfercho.orders.application.dto.PaymentMethod;
import com.superfercho.orders.application.dto.PaymentResult;
import com.superfercho.orders.application.dto.PaymentStatus;
import com.superfercho.orders.application.exception.OrderNotFoundException;
import com.superfercho.orders.application.port.CurrentUserProvider;
import com.superfercho.orders.application.port.CustomerDirectoryPort;
import com.superfercho.orders.application.port.OrderRepository;
import com.superfercho.orders.application.port.PaymentPort;
import com.superfercho.orders.domain.exception.InvalidOrderException;
import com.superfercho.orders.domain.model.Order;
import com.superfercho.orders.domain.model.OrderItem;
import com.superfercho.orders.domain.model.OrderNumber;
import com.superfercho.orders.domain.model.OrderStatus;
import com.superfercho.orders.domain.model.ShippingAddressSnapshot;
import com.superfercho.payments.application.exception.PaymentNotFoundException;
import com.superfercho.platform.money.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminOrderQueryUseCasesTest {

    private static final Instant CREATED_AT = Instant.parse("2026-03-01T10:00:00Z");
    private static final UUID CUSTOMER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_CUSTOMER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID UNKNOWN_CUSTOMER_ID = UUID.fromString("6f6f6f6f-6f6f-6f6f-6f6f-6f6f6f6f6f6f");
    private static final UUID ORDER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID PRODUCT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID PAYMENT_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final Money TOTAL = Money.cop(new BigDecimal("21.00"));
    private static final List<OrderStatus> SALES_STATUSES =
            List.of(OrderStatus.CONFIRMED, OrderStatus.PREPARING, OrderStatus.DELIVERY, OrderStatus.DELIVERED);

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentPort paymentPort;

    @Mock
    private CustomerDirectoryPort customerDirectoryPort;

    private GetAdminOrderUseCase getAdminOrder;
    private ListAdminOrdersUseCase listAdminOrders;

    @BeforeEach
    void setUp() {
        getAdminOrder = new GetAdminOrderUseCase(orderRepository, paymentPort, customerDirectoryPort);
        listAdminOrders = new ListAdminOrdersUseCase(orderRepository, customerDirectoryPort);
    }

    @Test
    void shouldReturnAnyCustomerOrderWithoutOwnershipCheck() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order(OTHER_CUSTOMER_ID, null)));
        when(customerDirectoryPort.findByUserIds(List.of(OTHER_CUSTOMER_ID))).thenReturn(Map.of());

        AdminOrderDetailResult result = getAdminOrder.execute(new GetOrderCommand(ORDER_ID));

        assertEquals(ORDER_ID, result.order().id());
        assertEquals(OTHER_CUSTOMER_ID, result.order().customerId());
        assertEquals("ORD-1001", result.order().orderNumber());
        assertNull(result.order().payment());
        assertNull(result.customer());
        verifyNoInteractions(currentUserProvider, paymentPort);
    }

    @Test
    void shouldEnrichAdminDetailCustomerWithSingleBatchCall() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order(CUSTOMER_ID, null)));
        when(customerDirectoryPort.findByUserIds(List.of(CUSTOMER_ID)))
                .thenReturn(Map.of(CUSTOMER_ID, entry(CUSTOMER_ID)));

        AdminOrderDetailResult result = getAdminOrder.execute(new GetOrderCommand(ORDER_ID));

        assertEquals(CUSTOMER_ID, result.customer().userId());
        assertEquals("Ada Lovelace", result.customer().fullName());
        assertEquals("CC", result.customer().documentType());
        assertEquals("123456789", result.customer().documentNumber());
        assertEquals("ada@example.com", result.customer().email());
        assertEquals("3001234567", result.customer().phone());
        verify(customerDirectoryPort).findByUserIds(List.of(CUSTOMER_ID));
        verifyNoMoreInteractions(customerDirectoryPort);
    }

    @Test
    void shouldComposePaymentForAdminOrderDetail() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order(OTHER_CUSTOMER_ID, PAYMENT_ID)));
        when(paymentPort.getPayment(PAYMENT_ID)).thenReturn(refundedCardPayment());
        when(customerDirectoryPort.findByUserIds(List.of(OTHER_CUSTOMER_ID))).thenReturn(Map.of());

        AdminOrderDetailResult result = getAdminOrder.execute(new GetOrderCommand(ORDER_ID));

        assertEquals(PAYMENT_ID, result.order().payment().paymentId());
        assertEquals(PaymentMethod.SIMULATED_CARD, result.order().payment().paymentMethod());
        assertEquals(TOTAL, result.order().payment().amount());
        assertEquals(PaymentStatus.APPROVED, result.order().payment().status());
        assertEquals("sim-1", result.order().payment().providerReference());
        assertEquals(CREATED_AT, result.order().payment().refundedAt());
        assertEquals(CREATED_AT, result.order().payment().createdAt());
        assertEquals(CREATED_AT, result.order().payment().updatedAt());
        verifyNoInteractions(currentUserProvider);
    }

    @Test
    void shouldKeepCodPaymentPendingOnDeliveredOrder() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(deliveredOrder(PAYMENT_ID)));
        when(paymentPort.getPayment(PAYMENT_ID)).thenReturn(codPayment());
        when(customerDirectoryPort.findByUserIds(List.of(CUSTOMER_ID))).thenReturn(Map.of());

        AdminOrderDetailResult result = getAdminOrder.execute(new GetOrderCommand(ORDER_ID));

        assertEquals(OrderStatus.DELIVERED, result.order().status());
        assertEquals(PaymentMethod.CASH_ON_DELIVERY, result.order().payment().paymentMethod());
        assertEquals(PaymentStatus.PENDING, result.order().payment().status());
        assertNull(result.order().payment().refundedAt());
    }

    @Test
    void shouldDegradeMissingPaymentToNullWithoutLosingTheOrder() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order(CUSTOMER_ID, PAYMENT_ID)));
        when(paymentPort.getPayment(PAYMENT_ID)).thenThrow(new PaymentNotFoundException(PAYMENT_ID));
        when(customerDirectoryPort.findByUserIds(List.of(CUSTOMER_ID))).thenReturn(Map.of());

        AdminOrderDetailResult result = getAdminOrder.execute(new GetOrderCommand(ORDER_ID));

        assertEquals(ORDER_ID, result.order().id());
        assertEquals(PAYMENT_ID, result.order().paymentId());
        assertNull(result.order().payment());
    }

    @Test
    void shouldRejectUnknownOrder() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> getAdminOrder.execute(new GetOrderCommand(ORDER_ID)));
        verifyNoInteractions(currentUserProvider, paymentPort, customerDirectoryPort);
    }

    @Test
    void shouldListAllOrdersWithPaginationDefaultsWhenStatusFilterIsAbsent() {
        Order order = order(CUSTOMER_ID, null);
        when(orderRepository.findByAdminFilter(
                        new AdminOrderFilter(List.of(), null, null, null), PageRequest.of(null, null)))
                .thenReturn(new PagedResult<>(List.of(order), 0, 20, 1));
        when(customerDirectoryPort.findByUserIds(List.of(CUSTOMER_ID)))
                .thenReturn(Map.of(CUSTOMER_ID, entry(CUSTOMER_ID)));

        PagedResult<AdminOrderListItemResult> result =
                listAdminOrders.execute(ListAdminOrdersCommand.of(null, null, null));

        assertEquals(1, result.items().size());
        assertEquals(ORDER_ID, result.items().get(0).order().id());
        assertNull(result.items().get(0).order().payment());
        assertEquals("Ada Lovelace", result.items().get(0).customer().fullName());
        assertEquals(0, result.page());
        assertEquals(20, result.size());
        verify(orderRepository).findByAdminFilter(new AdminOrderFilter(List.of(), null, null, null), new PageRequest(0, 20));
        verifyNoInteractions(currentUserProvider, paymentPort);
    }

    @Test
    void shouldResolvePageCustomersInSingleBatchCall() {
        Order first = order(CUSTOMER_ID, null);
        Order second = order(OTHER_CUSTOMER_ID, null);
        when(orderRepository.findByAdminFilter(any(), any()))
                .thenReturn(new PagedResult<>(List.of(first, second), 0, 20, 2));
        when(customerDirectoryPort.findByUserIds(List.of(CUSTOMER_ID, OTHER_CUSTOMER_ID)))
                .thenReturn(Map.of(
                        CUSTOMER_ID, entry(CUSTOMER_ID),
                        OTHER_CUSTOMER_ID, entry(OTHER_CUSTOMER_ID)));

        PagedResult<AdminOrderListItemResult> result =
                listAdminOrders.execute(ListAdminOrdersCommand.of(null, null, null));

        assertEquals(2, result.items().size());
        verify(customerDirectoryPort).findByUserIds(List.of(CUSTOMER_ID, OTHER_CUSTOMER_ID));
        verifyNoMoreInteractions(customerDirectoryPort);
    }

    @Test
    void shouldKeepRowWithoutCustomerWhenDirectoryCannotResolveIt() {
        Order order = order(UNKNOWN_CUSTOMER_ID, null);
        when(orderRepository.findByAdminFilter(any(), any()))
                .thenReturn(new PagedResult<>(List.of(order), 0, 20, 1));
        when(customerDirectoryPort.findByUserIds(List.of(UNKNOWN_CUSTOMER_ID))).thenReturn(Map.of());

        PagedResult<AdminOrderListItemResult> result =
                listAdminOrders.execute(ListAdminOrdersCommand.of(null, null, null));

        assertEquals(1, result.items().size());
        assertNull(result.items().get(0).customer());
        assertEquals(UNKNOWN_CUSTOMER_ID, result.items().get(0).order().customerId());
    }

    @Test
    void shouldNotCallCustomerDirectoryWhenPageIsEmpty() {
        when(orderRepository.findByAdminFilter(any(), any())).thenReturn(new PagedResult<>(List.of(), 0, 20, 0));

        PagedResult<AdminOrderListItemResult> result =
                listAdminOrders.execute(ListAdminOrdersCommand.of(null, null, null));

        assertEquals(0, result.items().size());
        verifyNoInteractions(customerDirectoryPort);
    }

    @Test
    void shouldFilterSalesStatusesAtPersistence() {
        when(orderRepository.findByAdminFilter(
                        new AdminOrderFilter(SALES_STATUSES, null, null, null), PageRequest.of(1, 10)))
                .thenReturn(new PagedResult<>(List.of(), 1, 10, 3));

        PagedResult<AdminOrderListItemResult> result = listAdminOrders.execute(ListAdminOrdersCommand.of(
                1, 10, List.of("CONFIRMED,PREPARING", "DELIVERY", "DELIVERED")));

        assertEquals(1, result.page());
        assertEquals(10, result.size());
        assertEquals(3, result.totalElements());
        verify(orderRepository).findByAdminFilter(
                new AdminOrderFilter(SALES_STATUSES, null, null, null), new PageRequest(1, 10));
        verifyNoInteractions(customerDirectoryPort, paymentPort);
    }

    @Test
    void shouldCapPageSizeAtMaximum() {
        when(orderRepository.findByAdminFilter(any(), any())).thenReturn(new PagedResult<>(List.of(), 2, 100, 0));

        PagedResult<AdminOrderListItemResult> result =
                listAdminOrders.execute(ListAdminOrdersCommand.of(2, 250, null));

        assertEquals(2, result.page());
        assertEquals(100, result.size());
        verify(orderRepository).findByAdminFilter(any(), eq(new PageRequest(2, 100)));
    }

    @Test
    void shouldPassOrderNumberSearchAndRangeToPersistence() {
        Instant from = Instant.parse("2026-03-01T05:00:00Z");
        Instant to = Instant.parse("2026-04-01T05:00:00Z");
        ListAdminOrdersCommand command =
                ListAdminOrdersCommand.of(null, null, List.of("DELIVERED"), "ord-1001", "2026-03-01", "2026-04-01");

        when(orderRepository.findByAdminFilter(any(), any())).thenReturn(new PagedResult<>(List.of(), 0, 20, 0));

        listAdminOrders.execute(command);

        verify(orderRepository).findByAdminFilter(
                new AdminOrderFilter(List.of(OrderStatus.DELIVERED), "ord-1001", from, to),
                new PageRequest(0, 20));
    }

    @Test
    void shouldRejectInvalidStatusFilter() {
        assertThrows(
                InvalidOrderException.class,
                () -> ListAdminOrdersCommand.of(0, 20, List.of("SOLD")));
        verifyNoInteractions(orderRepository);
    }

    private static CustomerDirectoryEntry entry(UUID userId) {
        return new CustomerDirectoryEntry(userId, "Ada Lovelace", "CC", "123456789", "ada@example.com", "3001234567");
    }

    private static Order order(UUID customerId, UUID paymentId) {
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
                paymentId,
                CREATED_AT,
                CREATED_AT);
    }

    private static Order deliveredOrder(UUID paymentId) {
        Instant at = CREATED_AT.plusSeconds(60);
        return order(CUSTOMER_ID, paymentId)
                .startPreparation(at)
                .startDelivery(at)
                .markDelivered(at);
    }

    private static PaymentResult refundedCardPayment() {
        return new PaymentResult(
                PAYMENT_ID,
                TOTAL,
                PaymentMethod.SIMULATED_CARD,
                PaymentStatus.APPROVED,
                "sim-1",
                CREATED_AT,
                CREATED_AT,
                CREATED_AT);
    }

    private static PaymentResult codPayment() {
        return new PaymentResult(
                PAYMENT_ID,
                TOTAL,
                PaymentMethod.CASH_ON_DELIVERY,
                PaymentStatus.PENDING,
                "cod-1",
                null,
                CREATED_AT,
                CREATED_AT);
    }
}
