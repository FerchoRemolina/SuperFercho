package com.superfercho.orders.infrastructure.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.superfercho.orders.application.dto.CancelOrderCommand;
import com.superfercho.orders.application.dto.CheckoutCommand;
import com.superfercho.orders.application.dto.CheckoutItem;
import com.superfercho.orders.application.dto.CheckoutResult;
import com.superfercho.orders.application.dto.GetOrderCommand;
import com.superfercho.orders.application.dto.ListOrdersCommand;
import com.superfercho.orders.application.dto.OrderItemResult;
import com.superfercho.orders.application.dto.OrderResult;
import com.superfercho.orders.application.dto.PagedResult;
import com.superfercho.orders.application.dto.PaymentMethod;
import com.superfercho.orders.application.dto.PaymentStatus;
import com.superfercho.orders.application.dto.ShippingAddressResult;
import com.superfercho.orders.application.usecase.GetOrderUseCase;
import com.superfercho.orders.application.usecase.ListOrdersUseCase;
import com.superfercho.orders.domain.model.OrderStatus;
import com.superfercho.orders.infrastructure.configuration.TransactionalCancelOrderUseCase;
import com.superfercho.orders.infrastructure.configuration.TransactionalCheckoutUseCase;
import com.superfercho.platform.error.ApiExceptionHandler;
import com.superfercho.platform.money.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({OrdersExceptionHandler.class, ApiExceptionHandler.class})
class OrderControllerTest {

    private static final Instant CREATED_AT = Instant.parse("2026-03-01T10:00:00Z");
    private static final UUID ORDER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID ADDRESS_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PRODUCT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID ITEM_ID = UUID.fromString("99999999-9999-9999-9999-000000000001");
    private static final UUID PAYMENT_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final Money PRICE = Money.cop(new BigDecimal("10.50"));
    private static final Money TOTAL = Money.cop(new BigDecimal("21.00"));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionalCheckoutUseCase transactionalCheckoutUseCase;

    @MockitoBean
    private TransactionalCancelOrderUseCase transactionalCancelOrderUseCase;

    @MockitoBean
    private GetOrderUseCase getOrderUseCase;

    @MockitoBean
    private ListOrdersUseCase listOrdersUseCase;

    @Test
    void shouldCheckoutWithIdempotencyKeyHeader() throws Exception {
        when(transactionalCheckoutUseCase.execute(any())).thenReturn(checkoutResult());

        mockMvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", "checkout-key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutJson()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/v1/orders/" + ORDER_ID)))
                .andExpect(jsonPath("$.orderId").value(ORDER_ID.toString()))
                .andExpect(jsonPath("$.orderNumber").value("ORD-P-1001"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.paymentStatus").value("APPROVED"))
                .andExpect(jsonPath("$.total.amount").value(21.00))
                .andExpect(jsonPath("$.total.currency").value("COP"));

        verify(transactionalCheckoutUseCase)
                .execute(new CheckoutCommand(
                        ADDRESS_ID,
                        PaymentMethod.SIMULATED_CARD,
                        List.of(new CheckoutItem(PRODUCT_ID, 2, PRICE)),
                        "checkout-key-1"));
        verifyNoInteractions(transactionalCancelOrderUseCase, getOrderUseCase, listOrdersUseCase);
    }

    @Test
    void shouldPassNullIdempotencyKeyWhenHeaderIsMissing() throws Exception {
        when(transactionalCheckoutUseCase.execute(any())).thenReturn(checkoutResult());

        mockMvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content(checkoutJson()))
                .andExpect(status().isCreated());

        verify(transactionalCheckoutUseCase)
                .execute(new CheckoutCommand(
                        ADDRESS_ID,
                        PaymentMethod.SIMULATED_CARD,
                        List.of(new CheckoutItem(PRODUCT_ID, 2, PRICE)),
                        null));
    }

    @Test
    void shouldGetOrderById() throws Exception {
        when(getOrderUseCase.execute(new GetOrderCommand(ORDER_ID))).thenReturn(orderResult(OrderStatus.PENDING, null));

        mockMvc.perform(get("/api/v1/orders/{orderId}", ORDER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ORDER_ID.toString()))
                .andExpect(jsonPath("$.orderNumber").value("ORD-P-1001"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.items[0].productId").value(PRODUCT_ID.toString()))
                .andExpect(jsonPath("$.items[0].productName").value("Leche entera"))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].unitPrice.amount").value(10.50))
                .andExpect(jsonPath("$.items[0].unitPrice.currency").value("COP"))
                .andExpect(jsonPath("$.subtotal.amount").value(21.00))
                .andExpect(jsonPath("$.total.currency").value("COP"))
                .andExpect(jsonPath("$.shippingAddress.city").value("Bogotá"))
                .andExpect(jsonPath("$.paymentId").value(PAYMENT_ID.toString()))
                .andExpect(jsonPath("$.createdAt").value(CREATED_AT.toString()))
                .andExpect(jsonPath("$.confirmedAt").isEmpty());

        verify(getOrderUseCase).execute(new GetOrderCommand(ORDER_ID));
        verifyNoInteractions(transactionalCheckoutUseCase, transactionalCancelOrderUseCase, listOrdersUseCase);
    }

    @Test
    void shouldListOrdersWithPageAndSize() throws Exception {
        when(listOrdersUseCase.execute(new ListOrdersCommand(1, 10)))
                .thenReturn(new PagedResult<>(List.of(orderResult(OrderStatus.PENDING, null)), 1, 10, 1));

        mockMvc.perform(get("/api/v1/orders").param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(ORDER_ID.toString()))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(listOrdersUseCase).execute(new ListOrdersCommand(1, 10));
        verify(listOrdersUseCase, never()).execute(new ListOrdersCommand(null, null));
        verifyNoInteractions(transactionalCheckoutUseCase, transactionalCancelOrderUseCase, getOrderUseCase);
    }

    @Test
    void shouldListOrdersWithoutPaginationParams() throws Exception {
        when(listOrdersUseCase.execute(new ListOrdersCommand(null, null)))
                .thenReturn(new PagedResult<>(List.of(), 0, 20, 0));

        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(0));

        verify(listOrdersUseCase).execute(new ListOrdersCommand(null, null));
    }

    @Test
    void shouldCancelOrder() throws Exception {
        when(transactionalCancelOrderUseCase.execute(new CancelOrderCommand(ORDER_ID)))
                .thenReturn(orderResult(OrderStatus.CANCELLED, CREATED_AT));

        mockMvc.perform(post("/api/v1/orders/{orderId}/cancel", ORDER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ORDER_ID.toString()))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelledAt").value(CREATED_AT.toString()));

        verify(transactionalCancelOrderUseCase).execute(new CancelOrderCommand(ORDER_ID));
        verifyNoInteractions(transactionalCheckoutUseCase, getOrderUseCase, listOrdersUseCase);
    }

    private static String checkoutJson() {
        return """
                {
                  "addressId": "%s",
                  "paymentMethod": "SIMULATED_CARD",
                  "items": [
                    {
                      "productId": "%s",
                      "quantity": 2,
                      "expectedUnitPrice": {"amount": 10.50, "currency": "COP"}
                    }
                  ]
                }
                """.formatted(ADDRESS_ID, PRODUCT_ID);
    }

    private static CheckoutResult checkoutResult() {
        return new CheckoutResult(ORDER_ID, "ORD-P-1001", OrderStatus.PENDING, PaymentStatus.APPROVED, TOTAL);
    }

    private static OrderResult orderResult(OrderStatus status, Instant cancelledAt) {
        return new OrderResult(
                ORDER_ID,
                "ORD-P-1001",
                status,
                List.of(new OrderItemResult(ITEM_ID, PRODUCT_ID, "Leche entera", PRICE, 2, TOTAL)),
                TOTAL,
                TOTAL,
                new ShippingAddressResult(
                        "Ada Lovelace", "Calle 1 # 2-3", "Apto 101", "Bogotá", "Cundinamarca", "3001234567"),
                PAYMENT_ID,
                CREATED_AT,
                null,
                cancelledAt,
                CREATED_AT);
    }
}
