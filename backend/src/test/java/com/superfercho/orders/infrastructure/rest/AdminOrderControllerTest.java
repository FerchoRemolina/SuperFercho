package com.superfercho.orders.infrastructure.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.superfercho.orders.application.dto.GetOrderCommand;
import com.superfercho.orders.application.dto.ListAdminOrdersCommand;
import com.superfercho.orders.application.dto.OrderItemResult;
import com.superfercho.orders.application.dto.OrderResult;
import com.superfercho.orders.application.dto.PagedResult;
import com.superfercho.orders.application.dto.PaymentMethod;
import com.superfercho.orders.application.dto.PaymentResult;
import com.superfercho.orders.application.dto.PaymentStatus;
import com.superfercho.orders.application.dto.ShippingAddressResult;
import com.superfercho.orders.application.exception.OrderNotFoundException;
import com.superfercho.orders.application.usecase.GetAdminOrderUseCase;
import com.superfercho.orders.application.usecase.ListAdminOrdersUseCase;
import com.superfercho.orders.domain.model.OrderStatus;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AdminOrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({OrdersExceptionHandler.class, ApiExceptionHandler.class})
class AdminOrderControllerTest {

    private static final Instant CREATED_AT = Instant.parse("2026-03-01T10:00:00Z");
    private static final UUID ORDER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID UNKNOWN_ORDER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID CUSTOMER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PRODUCT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID ITEM_ID = UUID.fromString("99999999-9999-9999-9999-000000000001");
    private static final UUID PAYMENT_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final Money PRICE = Money.cop(new BigDecimal("10.50"));
    private static final Money TOTAL = Money.cop(new BigDecimal("21.00"));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListAdminOrdersUseCase listAdminOrdersUseCase;

    @MockitoBean
    private GetAdminOrderUseCase getAdminOrderUseCase;

    @Test
    void shouldGetOrderByIdForAnyCustomer() throws Exception {
        when(getAdminOrderUseCase.execute(new GetOrderCommand(ORDER_ID))).thenReturn(orderResult(cardPayment()));

        mockMvc.perform(get("/api/v1/admin/orders/{orderId}", ORDER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ORDER_ID.toString()))
                .andExpect(jsonPath("$.orderNumber").value("ORD-P-1001"))
                .andExpect(jsonPath("$.customerId").value(CUSTOMER_ID.toString()))
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
                .andExpect(jsonPath("$.payment.paymentId").value(PAYMENT_ID.toString()))
                .andExpect(jsonPath("$.payment.paymentMethod").value("SIMULATED_CARD"))
                .andExpect(jsonPath("$.payment.amount.amount").value(21.00))
                .andExpect(jsonPath("$.payment.status").value("APPROVED"))
                .andExpect(jsonPath("$.payment.providerReference").value("sim-1"))
                .andExpect(jsonPath("$.payment.refundedAt").value(CREATED_AT.toString()))
                .andExpect(jsonPath("$.payment.createdAt").value(CREATED_AT.toString()))
                .andExpect(jsonPath("$.payment.updatedAt").value(CREATED_AT.toString()))
                .andExpect(jsonPath("$.createdAt").value(CREATED_AT.toString()));

        verify(getAdminOrderUseCase).execute(new GetOrderCommand(ORDER_ID));
        verifyNoInteractions(listAdminOrdersUseCase);
    }

    @Test
    void shouldMapUnknownOrderAsNotFound() throws Exception {
        when(getAdminOrderUseCase.execute(any())).thenThrow(new OrderNotFoundException(UNKNOWN_ORDER_ID));

        mockMvc.perform(get("/api/v1/admin/orders/{orderId}", UNKNOWN_ORDER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
    }

    @Test
    void shouldListOrdersWithPageAndSize() throws Exception {
        when(listAdminOrdersUseCase.execute(new ListAdminOrdersCommand(1, 10, List.of())))
                .thenReturn(new PagedResult<>(List.of(orderResult(null)), 1, 10, 1));

        mockMvc.perform(get("/api/v1/admin/orders").param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(ORDER_ID.toString()))
                .andExpect(jsonPath("$.items[0].customerId").value(CUSTOMER_ID.toString()))
                .andExpect(jsonPath("$.items[0].payment").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(listAdminOrdersUseCase).execute(new ListAdminOrdersCommand(1, 10, List.of()));
        verify(listAdminOrdersUseCase, never()).execute(ListAdminOrdersCommand.of(null, null, null));
        verifyNoInteractions(getAdminOrderUseCase);
    }

    @Test
    void shouldListOrdersWithoutPaginationParams() throws Exception {
        when(listAdminOrdersUseCase.execute(ListAdminOrdersCommand.of(null, null, null)))
                .thenReturn(new PagedResult<>(List.of(), 0, 20, 0));

        mockMvc.perform(get("/api/v1/admin/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(0));

        verify(listAdminOrdersUseCase).execute(ListAdminOrdersCommand.of(null, null, null));
    }

    @Test
    void shouldPassSalesStatusFilterToUseCase() throws Exception {
        ListAdminOrdersCommand command = ListAdminOrdersCommand.of(
                null, null, List.of("CONFIRMED", "PREPARING", "READY", "DELIVERED"));
        when(listAdminOrdersUseCase.execute(command)).thenReturn(new PagedResult<>(List.of(), 0, 20, 0));

        mockMvc.perform(get("/api/v1/admin/orders")
                        .param("status", "CONFIRMED")
                        .param("status", "PREPARING")
                        .param("status", "READY")
                        .param("status", "DELIVERED"))
                .andExpect(status().isOk());

        verify(listAdminOrdersUseCase).execute(command);
    }

    @Test
    void shouldPassCommaSeparatedStatusFilterToUseCase() throws Exception {
        ListAdminOrdersCommand command =
                ListAdminOrdersCommand.of(null, null, List.of("CONFIRMED,PREPARING,READY,DELIVERED"));
        when(listAdminOrdersUseCase.execute(command)).thenReturn(new PagedResult<>(List.of(), 0, 20, 0));

        mockMvc.perform(get("/api/v1/admin/orders").param("status", "CONFIRMED,PREPARING,READY,DELIVERED"))
                .andExpect(status().isOk());

        verify(listAdminOrdersUseCase).execute(command);
    }

    @Test
    void shouldRejectInvalidStatusFilter() throws Exception {
        mockMvc.perform(get("/api/v1/admin/orders").param("status", "SOLD"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ORDER"));

        verifyNoInteractions(listAdminOrdersUseCase, getAdminOrderUseCase);
    }

    private static OrderResult orderResult(PaymentResult payment) {
        return new OrderResult(
                ORDER_ID,
                "ORD-P-1001",
                CUSTOMER_ID,
                OrderStatus.PENDING,
                List.of(new OrderItemResult(ITEM_ID, PRODUCT_ID, "Leche entera", PRICE, 2, TOTAL)),
                TOTAL,
                TOTAL,
                new ShippingAddressResult(
                        "Ada Lovelace", "Calle 1 # 2-3", "Apto 101", "Bogotá", "Cundinamarca", "3001234567"),
                PAYMENT_ID,
                CREATED_AT,
                null,
                null,
                CREATED_AT,
                payment);
    }

    private static PaymentResult cardPayment() {
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
}
