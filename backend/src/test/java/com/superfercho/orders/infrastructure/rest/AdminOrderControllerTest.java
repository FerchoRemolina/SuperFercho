package com.superfercho.orders.infrastructure.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.superfercho.orders.application.dto.AdminCustomerSalesRow;
import com.superfercho.orders.application.dto.AdminOrderDetailResult;
import com.superfercho.orders.application.dto.AdminOrderListItemResult;
import com.superfercho.orders.application.dto.AdminOrderPeriodSummaryResult;
import com.superfercho.orders.application.dto.AdminProductSalesRow;
import com.superfercho.orders.application.dto.AdminRecentBuyerResult;
import com.superfercho.orders.application.dto.AdminSalesAnalyticsResult;
import com.superfercho.orders.application.dto.AdminSalesBucketResult;
import com.superfercho.orders.application.dto.AdminSalesPeriodSummaryResult;
import com.superfercho.orders.application.dto.CustomerDirectoryEntry;
import com.superfercho.orders.application.dto.GetAdminDashboardSummaryCommand;
import com.superfercho.orders.application.dto.GetAdminSalesPeriodAnalyticsCommand;
import com.superfercho.orders.application.dto.GetAdminSalesPeriodSummaryCommand;
import com.superfercho.orders.application.dto.GetAdminTopCustomersCommand;
import com.superfercho.orders.application.dto.GetAdminTopProductsCommand;
import com.superfercho.orders.application.dto.GetOrderCommand;
import com.superfercho.orders.application.dto.ListAdminOrdersCommand;
import com.superfercho.orders.application.dto.ListAdminRecentBuyersCommand;
import com.superfercho.orders.application.dto.OrderItemResult;
import com.superfercho.orders.application.dto.OrderResult;
import com.superfercho.orders.application.dto.PagedResult;
import com.superfercho.orders.application.dto.PaymentMethod;
import com.superfercho.orders.application.dto.PaymentResult;
import com.superfercho.orders.application.dto.PaymentStatus;
import com.superfercho.orders.application.dto.ShippingAddressResult;
import com.superfercho.orders.application.exception.OrderNotFoundException;
import com.superfercho.orders.application.usecase.GetAdminOrderPeriodSummaryUseCase;
import com.superfercho.orders.application.usecase.GetAdminOrderUseCase;
import com.superfercho.orders.application.usecase.GetAdminSalesPeriodAnalyticsUseCase;
import com.superfercho.orders.application.usecase.GetAdminSalesPeriodSummaryUseCase;
import com.superfercho.orders.application.usecase.GetAdminTopCustomersUseCase;
import com.superfercho.orders.application.usecase.GetAdminTopProductsUseCase;
import com.superfercho.orders.application.usecase.ListAdminOrdersUseCase;
import com.superfercho.orders.application.usecase.ListAdminRecentBuyersUseCase;
import com.superfercho.orders.domain.model.OrderStatus;
import com.superfercho.orders.domain.model.SalesPeriodGranularity;
import com.superfercho.platform.error.ApiExceptionHandler;
import com.superfercho.platform.money.Money;
import com.superfercho.platform.time.BucketGranularity;
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

    @MockitoBean
    private GetAdminSalesPeriodSummaryUseCase getAdminSalesPeriodSummaryUseCase;

    @MockitoBean
    private ListAdminRecentBuyersUseCase listAdminRecentBuyersUseCase;

    @MockitoBean
    private GetAdminSalesPeriodAnalyticsUseCase getAdminSalesPeriodAnalyticsUseCase;

    @MockitoBean
    private GetAdminOrderPeriodSummaryUseCase getAdminOrderPeriodSummaryUseCase;

    @MockitoBean
    private GetAdminTopProductsUseCase getAdminTopProductsUseCase;

    @MockitoBean
    private GetAdminTopCustomersUseCase getAdminTopCustomersUseCase;

    @Test
    void shouldGetOrderByIdForAnyCustomer() throws Exception {
        when(getAdminOrderUseCase.execute(new GetOrderCommand(ORDER_ID)))
                .thenReturn(new AdminOrderDetailResult(orderResult(cardPayment()), customerEntry()));

        mockMvc.perform(get("/api/v1/admin/orders/{orderId}", ORDER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ORDER_ID.toString()))
                .andExpect(jsonPath("$.orderNumber").value("ORD-P-1001"))
                .andExpect(jsonPath("$.customerId").value(CUSTOMER_ID.toString()))
                .andExpect(jsonPath("$.customer.fullName").value("Ada Lovelace"))
                .andExpect(jsonPath("$.customer.documentType").value("CC"))
                .andExpect(jsonPath("$.customer.documentNumber").value("123456789"))
                .andExpect(jsonPath("$.customer.email").value("ada@example.com"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.confirmedAt").value(CREATED_AT.toString()))
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
    void shouldRenderDetailWithoutResolvedCustomer() throws Exception {
        when(getAdminOrderUseCase.execute(new GetOrderCommand(ORDER_ID)))
                .thenReturn(new AdminOrderDetailResult(orderResult(null), null));

        mockMvc.perform(get("/api/v1/admin/orders/{orderId}", ORDER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customer").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.customerId").value(CUSTOMER_ID.toString()))
                .andExpect(jsonPath("$.payment").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void shouldKeepOrderVisibleWhenPaymentIsMissing() throws Exception {
        when(getAdminOrderUseCase.execute(new GetOrderCommand(ORDER_ID)))
                .thenReturn(new AdminOrderDetailResult(orderResult(null), customerEntry()));

        mockMvc.perform(get("/api/v1/admin/orders/{orderId}", ORDER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ORDER_ID.toString()))
                .andExpect(jsonPath("$.paymentId").value(PAYMENT_ID.toString()))
                .andExpect(jsonPath("$.payment").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.customer.fullName").value("Ada Lovelace"));
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
        when(listAdminOrdersUseCase.execute(new ListAdminOrdersCommand(1, 10, List.of(), null, null, null)))
                .thenReturn(new PagedResult<>(List.of(listItem()), 1, 10, 1));

        mockMvc.perform(get("/api/v1/admin/orders").param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(ORDER_ID.toString()))
                .andExpect(jsonPath("$.items[0].customerId").value(CUSTOMER_ID.toString()))
                .andExpect(jsonPath("$.items[0].customer.fullName").value("Ada Lovelace"))
                .andExpect(jsonPath("$.items[0].customer.documentType").value("CC"))
                .andExpect(jsonPath("$.items[0].items[0].productName").value("Leche entera"))
                .andExpect(jsonPath("$.items[0].shippingRecipientName").value("Ada Lovelace"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(listAdminOrdersUseCase).execute(new ListAdminOrdersCommand(1, 10, List.of(), null, null, null));
        verify(listAdminOrdersUseCase, never())
                .execute(ListAdminOrdersCommand.of(null, null, null, null, null, null));
        verifyNoInteractions(getAdminOrderUseCase);
    }

    @Test
    void shouldListOrdersWithoutPaginationParams() throws Exception {
        when(listAdminOrdersUseCase.execute(new ListAdminOrdersCommand(null, null, List.of(), null, null, null)))
                .thenReturn(new PagedResult<>(List.of(), 0, 20, 0));

        mockMvc.perform(get("/api/v1/admin/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(0));

        verify(listAdminOrdersUseCase).execute(new ListAdminOrdersCommand(null, null, List.of(), null, null, null));
    }

    @Test
    void shouldPassSalesStatusFilterToUseCase() throws Exception {
        ListAdminOrdersCommand command = ListAdminOrdersCommand.of(
                null, null, List.of("CONFIRMED", "PREPARING", "DELIVERY", "DELIVERED"), null, null, null);
        when(listAdminOrdersUseCase.execute(command)).thenReturn(new PagedResult<>(List.of(), 0, 20, 0));

        mockMvc.perform(get("/api/v1/admin/orders")
                        .param("status", "CONFIRMED")
                        .param("status", "PREPARING")
                        .param("status", "DELIVERY")
                        .param("status", "DELIVERED"))
                .andExpect(status().isOk());

        verify(listAdminOrdersUseCase).execute(command);
    }

    @Test
    void shouldPassCommaSeparatedStatusFilterToUseCase() throws Exception {
        ListAdminOrdersCommand command = ListAdminOrdersCommand.of(
                null, null, List.of("CONFIRMED,PREPARING,DELIVERY,DELIVERED"), null, null, null);
        when(listAdminOrdersUseCase.execute(command)).thenReturn(new PagedResult<>(List.of(), 0, 20, 0));

        mockMvc.perform(get("/api/v1/admin/orders").param("status", "CONFIRMED,PREPARING,DELIVERY,DELIVERED"))
                .andExpect(status().isOk());

        verify(listAdminOrdersUseCase).execute(command);
    }

    @Test
    void shouldPassOrderNumberSearchToUseCase() throws Exception {
        ListAdminOrdersCommand command =
                ListAdminOrdersCommand.of(null, null, null, "ORD-ABC", null, null);
        when(listAdminOrdersUseCase.execute(command)).thenReturn(new PagedResult<>(List.of(), 0, 20, 0));

        mockMvc.perform(get("/api/v1/admin/orders").param("orderNumber", " ORD-ABC "))
                .andExpect(status().isOk());

        verify(listAdminOrdersUseCase).execute(command);
    }

    @Test
    void shouldPassDateRangeToUseCase() throws Exception {
        ListAdminOrdersCommand command =
                ListAdminOrdersCommand.of(null, null, null, null, "2026-03-01", "2026-04-01");
        when(listAdminOrdersUseCase.execute(command)).thenReturn(new PagedResult<>(List.of(), 0, 20, 0));

        mockMvc.perform(get("/api/v1/admin/orders")
                        .param("from", "2026-03-01")
                        .param("to", "2026-04-01"))
                .andExpect(status().isOk());

        verify(listAdminOrdersUseCase).execute(command);
    }

    @Test
    void shouldCombineSearchStatusAndDateRange() throws Exception {
        ListAdminOrdersCommand command =
                ListAdminOrdersCommand.of(0, 50, List.of("DELIVERED"), "ORD-1001", "2026-03-01", "2026-04-01");
        when(listAdminOrdersUseCase.execute(command)).thenReturn(new PagedResult<>(List.of(), 0, 50, 0));

        mockMvc.perform(get("/api/v1/admin/orders")
                        .param("page", "0")
                        .param("size", "50")
                        .param("status", "DELIVERED")
                        .param("orderNumber", "ORD-1001")
                        .param("from", "2026-03-01")
                        .param("to", "2026-04-01"))
                .andExpect(status().isOk());

        verify(listAdminOrdersUseCase).execute(command);
    }

    @Test
    void shouldRejectInvalidDateBound() throws Exception {
        mockMvc.perform(get("/api/v1/admin/orders").param("from", "01/03/2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ORDER"));

        verifyNoInteractions(listAdminOrdersUseCase, getAdminOrderUseCase);
    }

    @Test
    void shouldRejectRangeWithFromAfterTo() throws Exception {
        mockMvc.perform(get("/api/v1/admin/orders")
                        .param("from", "2026-04-01")
                        .param("to", "2026-03-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ORDER"));

        verifyNoInteractions(listAdminOrdersUseCase, getAdminOrderUseCase);
    }

    @Test
    void shouldRenderRowWithoutResolvedCustomer() throws Exception {
        when(listAdminOrdersUseCase.execute(new ListAdminOrdersCommand(null, null, List.of(), null, null, null)))
                .thenReturn(new PagedResult<>(List.of(new AdminOrderListItemResult(orderResult(null), null)), 0, 20, 1));

        mockMvc.perform(get("/api/v1/admin/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].customer").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.items[0].customerId").value(CUSTOMER_ID.toString()));
    }

    @Test
    void shouldRejectInvalidStatusFilter() throws Exception {
        mockMvc.perform(get("/api/v1/admin/orders").param("status", "SOLD"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ORDER"));

        verifyNoInteractions(listAdminOrdersUseCase, getAdminOrderUseCase);
    }

    @Test
    void shouldReturnSalesPeriodSummaryForGranularity() throws Exception {
        when(getAdminSalesPeriodSummaryUseCase.execute(
                        new GetAdminSalesPeriodSummaryCommand(SalesPeriodGranularity.MONTH)))
                .thenReturn(new AdminSalesPeriodSummaryResult(
                        SalesPeriodGranularity.MONTH,
                        List.of(new AdminSalesBucketResult(
                                Instant.parse("2026-03-01T00:00:00Z"), "01 mar.", TOTAL, 2))));

        mockMvc.perform(get("/api/v1/admin/orders/dashboard/sales").param("granularity", "MONTH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.granularity").value("MONTH"))
                .andExpect(jsonPath("$.buckets[0].label").value("01 mar."))
                .andExpect(jsonPath("$.buckets[0].orderCount").value(2))
                .andExpect(jsonPath("$.buckets[0].total.amount").value(21.00))
                .andExpect(jsonPath("$.buckets[0].total.currency").value("COP"));

        verify(getAdminSalesPeriodSummaryUseCase)
                .execute(new GetAdminSalesPeriodSummaryCommand(SalesPeriodGranularity.MONTH));
        verifyNoInteractions(listAdminOrdersUseCase, getAdminOrderUseCase, listAdminRecentBuyersUseCase);
    }

    @Test
    void shouldDefaultSalesGranularityToWeek() throws Exception {
        when(getAdminSalesPeriodSummaryUseCase.execute(GetAdminSalesPeriodSummaryCommand.of(null)))
                .thenReturn(new AdminSalesPeriodSummaryResult(SalesPeriodGranularity.WEEK, List.of()));

        mockMvc.perform(get("/api/v1/admin/orders/dashboard/sales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.granularity").value("WEEK"))
                .andExpect(jsonPath("$.buckets").isEmpty());

        verify(getAdminSalesPeriodSummaryUseCase).execute(GetAdminSalesPeriodSummaryCommand.of(null));
    }

    @Test
    void shouldRejectUnsupportedSalesGranularity() throws Exception {
        mockMvc.perform(get("/api/v1/admin/orders/dashboard/sales").param("granularity", "QUARTER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ORDER"));

        verifyNoInteractions(getAdminSalesPeriodSummaryUseCase);
    }

    @Test
    void shouldReturnRecentBuyers() throws Exception {
        when(listAdminRecentBuyersUseCase.execute(ListAdminRecentBuyersCommand.of(5)))
                .thenReturn(List.of(new AdminRecentBuyerResult(
                        CUSTOMER_ID, "Ada Lovelace", CREATED_AT, 3, TOTAL)));

        mockMvc.perform(get("/api/v1/admin/orders/dashboard/recent-buyers").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].customerId").value(CUSTOMER_ID.toString()))
                .andExpect(jsonPath("$.items[0].displayName").value("Ada Lovelace"))
                .andExpect(jsonPath("$.items[0].orderCount").value(3))
                .andExpect(jsonPath("$.items[0].lastOrderTotal.amount").value(21.00));

        verify(listAdminRecentBuyersUseCase).execute(ListAdminRecentBuyersCommand.of(5));
        verifyNoInteractions(listAdminOrdersUseCase, getAdminOrderUseCase, getAdminSalesPeriodSummaryUseCase);
    }

    @Test
    void shouldReturnSalesForArbitraryPeriod() throws Exception {
        GetAdminSalesPeriodAnalyticsCommand command = GetAdminSalesPeriodAnalyticsCommand.of(
                "2026-05-01T00:00:00", "2026-05-04T00:00:00", "DAY");
        when(getAdminSalesPeriodAnalyticsUseCase.execute(command))
                .thenReturn(new AdminSalesAnalyticsResult(
                        BucketGranularity.DAY,
                        List.of(new AdminSalesBucketResult(
                                Instant.parse("2026-05-01T05:00:00Z"), "01 may.", TOTAL, 8))));

        mockMvc.perform(get("/api/v1/admin/orders/dashboard/sales")
                        .param("from", "2026-05-01T00:00:00")
                        .param("to", "2026-05-04T00:00:00")
                        .param("granularity", "DAY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.granularity").value("DAY"))
                .andExpect(jsonPath("$.buckets[0].periodStart").value("2026-05-01T05:00:00Z"))
                .andExpect(jsonPath("$.buckets[0].label").value("01 may."))
                .andExpect(jsonPath("$.buckets[0].orderCount").value(8))
                .andExpect(jsonPath("$.buckets[0].total.amount").value(21.00))
                .andExpect(jsonPath("$.buckets[0].total.currency").value("COP"));

        verify(getAdminSalesPeriodAnalyticsUseCase).execute(command);
        verifyNoInteractions(getAdminSalesPeriodSummaryUseCase);
    }

    @Test
    void shouldRejectArbitrarySalesPeriodWithMissingBound() throws Exception {
        mockMvc.perform(get("/api/v1/admin/orders/dashboard/sales").param("from", "2026-05-01T00:00:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SALES_PERIOD"));

        verifyNoInteractions(getAdminSalesPeriodAnalyticsUseCase, getAdminSalesPeriodSummaryUseCase);
    }

    @Test
    void shouldReturnDashboardSummaryForArbitraryPeriod() throws Exception {
        GetAdminDashboardSummaryCommand command =
                GetAdminDashboardSummaryCommand.of("2026-05-01T00:00:00", "2026-06-01T00:00:00");
        when(getAdminOrderPeriodSummaryUseCase.execute(command))
                .thenReturn(new AdminOrderPeriodSummaryResult(Money.cop(new BigDecimal("1234500.00")), 49, 10, 35, 4));

        mockMvc.perform(get("/api/v1/admin/orders/dashboard/summary")
                        .param("from", "2026-05-01T00:00:00")
                        .param("to", "2026-06-01T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("2026-05-01T05:00:00Z"))
                .andExpect(jsonPath("$.to").value("2026-06-01T05:00:00Z"))
                .andExpect(jsonPath("$.sales.amount").value(1234500.00))
                .andExpect(jsonPath("$.sales.currency").value("COP"))
                .andExpect(jsonPath("$.totalOrders").value(49))
                .andExpect(jsonPath("$.inProcessOrders").value(10))
                .andExpect(jsonPath("$.deliveredOrders").value(35))
                .andExpect(jsonPath("$.cancelledOrders").value(4));

        verify(getAdminOrderPeriodSummaryUseCase).execute(command);
    }

    @Test
    void shouldRejectSwappedDashboardSummaryPeriod() throws Exception {
        mockMvc.perform(get("/api/v1/admin/orders/dashboard/summary")
                        .param("from", "2026-06-01T00:00:00")
                        .param("to", "2026-05-01T00:00:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SALES_PERIOD"));

        verifyNoInteractions(getAdminOrderPeriodSummaryUseCase);
    }

    @Test
    void shouldReturnTopProductsForArbitraryPeriod() throws Exception {
        GetAdminTopProductsCommand command =
                GetAdminTopProductsCommand.of("2026-05-01", "2026-06-01", 3, "ASC");
        when(getAdminTopProductsUseCase.execute(command))
                .thenReturn(List.of(new AdminProductSalesRow(PRODUCT_ID, "Leche entera", 25)));

        mockMvc.perform(get("/api/v1/admin/orders/dashboard/products")
                        .param("from", "2026-05-01")
                        .param("to", "2026-06-01")
                        .param("limit", "3")
                        .param("sort", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].productId").value(PRODUCT_ID.toString()))
                .andExpect(jsonPath("$.items[0].productName").value("Leche entera"))
                .andExpect(jsonPath("$.items[0].quantity").value(25));

        verify(getAdminTopProductsUseCase).execute(command);
    }

    @Test
    void shouldReturnTopCustomersForArbitraryPeriod() throws Exception {
        GetAdminTopCustomersCommand command =
                GetAdminTopCustomersCommand.of("2026-05-01", "2026-06-01", 5, null);
        when(getAdminTopCustomersUseCase.execute(command))
                .thenReturn(List.of(new AdminCustomerSalesRow(
                        CUSTOMER_ID, "Ada Lovelace", new BigDecimal("450000.00"), 6)));

        mockMvc.perform(get("/api/v1/admin/orders/dashboard/customers/top")
                        .param("from", "2026-05-01")
                        .param("to", "2026-06-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].customerId").value(CUSTOMER_ID.toString()))
                .andExpect(jsonPath("$.items[0].customerName").value("Ada Lovelace"))
                .andExpect(jsonPath("$.items[0].total.amount").value(450000.00))
                .andExpect(jsonPath("$.items[0].orderCount").value(6));

        verify(getAdminTopCustomersUseCase).execute(command);
    }

    @Test
    void shouldReturnTopCustomersSortedByOrderCount() throws Exception {
        GetAdminTopCustomersCommand command =
                GetAdminTopCustomersCommand.of("2026-05-01", "2026-06-01", 5, "ORDERS");
        when(getAdminTopCustomersUseCase.execute(command))
                .thenReturn(List.of(new AdminCustomerSalesRow(
                        CUSTOMER_ID, "Ada Lovelace", new BigDecimal("120000.00"), 9)));

        mockMvc.perform(get("/api/v1/admin/orders/dashboard/customers/top")
                        .param("from", "2026-05-01")
                        .param("to", "2026-06-01")
                        .param("sort", "ORDERS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].orderCount").value(9));

        verify(getAdminTopCustomersUseCase).execute(command);
    }

    private static AdminOrderListItemResult listItem() {
        return new AdminOrderListItemResult(
                orderResult(null),
                new CustomerDirectoryEntry(CUSTOMER_ID, "Ada Lovelace", "CC", "123456789", "ada@example.com", null));
    }

    private static CustomerDirectoryEntry customerEntry() {
        return new CustomerDirectoryEntry(CUSTOMER_ID, "Ada Lovelace", "CC", "123456789", "ada@example.com", null);
    }

    private static OrderResult orderResult(PaymentResult payment) {
        return new OrderResult(
                ORDER_ID,
                "ORD-P-1001",
                CUSTOMER_ID,
                OrderStatus.CONFIRMED,
                List.of(new OrderItemResult(ITEM_ID, PRODUCT_ID, "Leche entera", PRICE, 2, TOTAL)),
                TOTAL,
                TOTAL,
                new ShippingAddressResult(
                        "Ada Lovelace", "Calle 1 # 2-3", "Apto 101", "Bogotá", "Cundinamarca", "3001234567"),
                PAYMENT_ID,
                CREATED_AT,
                CREATED_AT,
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
