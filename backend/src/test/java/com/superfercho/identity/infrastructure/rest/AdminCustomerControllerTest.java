package com.superfercho.identity.infrastructure.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.superfercho.identity.application.dto.AdminCustomerAccountResult;
import com.superfercho.identity.application.dto.AdminCustomerRecordListItem;
import com.superfercho.identity.application.dto.AdminCustomerRecordResult;
import com.superfercho.identity.application.dto.AdminNewCustomersResult;
import com.superfercho.identity.application.dto.AdminCustomerRecordSearchCriteria;
import com.superfercho.identity.application.dto.AdminCustomerRecordsPage;
import com.superfercho.identity.application.dto.AdminPagedResult;
import com.superfercho.identity.application.dto.CustomerCommercialOrderView;
import com.superfercho.identity.application.dto.CustomerCommercialPaymentView;
import com.superfercho.identity.application.dto.GetAdminNewCustomersCommand;
import com.superfercho.identity.application.exception.CustomerRecordNotFoundException;
import com.superfercho.identity.application.usecase.ActivateAdminCustomerAccountUseCase;
import com.superfercho.identity.application.usecase.DeactivateAdminCustomerAccountUseCase;
import com.superfercho.identity.application.usecase.FindAdminCustomerByDocumentUseCase;
import com.superfercho.identity.application.usecase.GetAdminCustomerRecordUseCase;
import com.superfercho.identity.application.usecase.GetAdminNewCustomersUseCase;
import com.superfercho.identity.application.usecase.ListAdminCustomerOrdersUseCase;
import com.superfercho.identity.application.usecase.ListAdminCustomerPaymentsUseCase;
import com.superfercho.identity.application.usecase.SearchAdminCustomerRecordsUseCase;
import com.superfercho.identity.application.dto.AdminCustomerRecordSortBy;
import com.superfercho.identity.application.dto.AdminCustomerRecordSortDir;
import com.superfercho.identity.application.dto.AdminCustomerRecordStatus;
import com.superfercho.identity.application.dto.AdminCustomerRecordStatusFilter;
import com.superfercho.identity.domain.model.UserStatus;
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

@WebMvcTest(controllers = AdminCustomerController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(IdentityExceptionHandler.class)
class AdminCustomerControllerTest {

    private static final Instant NOW = Instant.parse("2026-04-01T12:00:00Z");
    private static final UUID RECORD_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ORDER_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final UUID PAYMENT_ID = UUID.fromString("88888888-8888-8888-8888-888888888888");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FindAdminCustomerByDocumentUseCase findAdminCustomerByDocumentUseCase;

    @MockitoBean
    private GetAdminCustomerRecordUseCase getAdminCustomerRecordUseCase;

    @MockitoBean
    private ListAdminCustomerOrdersUseCase listAdminCustomerOrdersUseCase;

    @MockitoBean
    private ListAdminCustomerPaymentsUseCase listAdminCustomerPaymentsUseCase;

    @MockitoBean
    private ActivateAdminCustomerAccountUseCase activateAdminCustomerAccountUseCase;

    @MockitoBean
    private DeactivateAdminCustomerAccountUseCase deactivateAdminCustomerAccountUseCase;

    @MockitoBean
    private GetAdminNewCustomersUseCase getAdminNewCustomersUseCase;

    @MockitoBean
    private SearchAdminCustomerRecordsUseCase searchAdminCustomerRecordsUseCase;

    @Test
    void shouldFindByDocument() throws Exception {
        when(findAdminCustomerByDocumentUseCase.execute(any())).thenReturn(recordResult());

        mockMvc.perform(get("/api/v1/admin/customers/by-document")
                        .param("documentType", "CC")
                        .param("documentNumber", "100200300")
                        .param("accountStatus", "ALL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(RECORD_ID.toString()))
                .andExpect(jsonPath("$.documentType").value("CC"))
                .andExpect(jsonPath("$.accounts[0].id").value(USER_ID.toString()))
                .andExpect(jsonPath("$.accounts[0].email").value("ada@example.com"))
                .andExpect(jsonPath("$.accounts[0].passwordHash").doesNotExist());
    }

    @Test
    void shouldReturn404WhenDocumentMissing() throws Exception {
        when(findAdminCustomerByDocumentUseCase.execute(any()))
                .thenThrow(new CustomerRecordNotFoundException("CC", "00000000"));

        mockMvc.perform(get("/api/v1/admin/customers/by-document")
                        .param("documentType", "CC")
                        .param("documentNumber", "00000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CUSTOMER_RECORD_NOT_FOUND"));
    }

    @Test
    void shouldListOrdersAndPayments() throws Exception {
        Money amount = Money.cop(new BigDecimal("10000"));
        when(listAdminCustomerOrdersUseCase.execute(any()))
                .thenReturn(new AdminPagedResult<>(
                        List.of(new CustomerCommercialOrderView(
                                ORDER_ID,
                                "ORD-1",
                                USER_ID,
                                "CONFIRMED",
                                amount,
                                amount,
                                PAYMENT_ID,
                                NOW,
                                NOW,
                                null,
                                NOW)),
                        0,
                        20,
                        1));
        when(listAdminCustomerPaymentsUseCase.execute(any()))
                .thenReturn(new AdminPagedResult<>(
                        List.of(new CustomerCommercialPaymentView(
                                PAYMENT_ID,
                                ORDER_ID,
                                amount,
                                "SIMULATED_CARD",
                                "APPROVED",
                                "ref",
                                NOW,
                                NOW,
                                null)),
                        0,
                        20,
                        1));

        mockMvc.perform(get("/api/v1/admin/customers/{id}/orders", RECORD_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(ORDER_ID.toString()))
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/v1/admin/customers/{id}/payments", RECORD_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].orderId").value(ORDER_ID.toString()))
                .andExpect(jsonPath("$.items[0].id").value(PAYMENT_ID.toString()));
    }

    @Test
    void shouldActivateAndDeactivate() throws Exception {
        when(activateAdminCustomerAccountUseCase.execute(any())).thenReturn(accountResult(UserStatus.ACTIVE));
        when(deactivateAdminCustomerAccountUseCase.execute(any())).thenReturn(accountResult(UserStatus.INACTIVE));

        mockMvc.perform(post(
                        "/api/v1/admin/customers/{recordId}/accounts/{userId}/activate",
                        RECORD_ID,
                        USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        mockMvc.perform(post(
                        "/api/v1/admin/customers/{recordId}/accounts/{userId}/deactivate",
                        RECORD_ID,
                        USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        verify(activateAdminCustomerAccountUseCase).execute(any());
        verify(deactivateAdminCustomerAccountUseCase).execute(any());
    }

    @Test
    void shouldReturnNewCustomersForArbitraryPeriod() throws Exception {
        GetAdminNewCustomersCommand command =
                GetAdminNewCustomersCommand.of("2026-05-01T00:00:00", "2026-06-01T00:00:00", "DAY");
        when(getAdminNewCustomersUseCase.execute(command))
                .thenReturn(new AdminNewCustomersResult(4, List.of(
                        new AdminNewCustomersResult.AdminNewCustomersBucket(
                                Instant.parse("2026-05-01T05:00:00Z"), "01 may.", 4))));

        mockMvc.perform(get("/api/v1/admin/customers/dashboard/new")
                        .param("from", "2026-05-01T00:00:00")
                        .param("to", "2026-06-01T00:00:00")
                        .param("granularity", "DAY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(4))
                .andExpect(jsonPath("$.buckets[0].periodStart").value("2026-05-01T05:00:00Z"))
                .andExpect(jsonPath("$.buckets[0].label").value("01 may."))
                .andExpect(jsonPath("$.buckets[0].count").value(4));

        verify(getAdminNewCustomersUseCase).execute(command);
    }

    @Test
    void shouldListCustomerRecords() throws Exception {
        var item = new AdminCustomerRecordListItem(
                RECORD_ID, "CC", "123456789", "Ana", "Gómez",
                "ana@example.com", "3000000000",
                AdminCustomerRecordStatus.ACTIVE,
                2, 1, 1, 0,
                12, com.superfercho.platform.money.Money.cop(new java.math.BigDecimal("863650")),
                Instant.parse("2026-09-25T12:00:00Z"), NOW, NOW);
        when(searchAdminCustomerRecordsUseCase.execute(any())).thenReturn(
                new AdminCustomerRecordsPage(List.of(item), 0, 20, 1));

        mockMvc.perform(get("/api/v1/admin/customers")
                        .param("search", "ana")
                        .param("status", "ALL")
                        .param("sortBy", "NAME")
                        .param("sortDir", "ASC")
                        .param("hasPurchases", "false")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(RECORD_ID.toString()))
                .andExpect(jsonPath("$.items[0].email").value("ana@example.com"))
                .andExpect(jsonPath("$.items[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.items[0].orderCount").value(12))
                .andExpect(jsonPath("$.totalElements").value(1));

        var expected = new AdminCustomerRecordSearchCriteria(
                "ana", AdminCustomerRecordStatusFilter.ALL,
                AdminCustomerRecordSortBy.NAME,
                AdminCustomerRecordSortDir.ASC, Boolean.FALSE, 0, 20);
        verify(searchAdminCustomerRecordsUseCase).execute(expected);
    }

    @Test
    void shouldRejectInvalidNewCustomersPeriod() throws Exception {
        mockMvc.perform(get("/api/v1/admin/customers/dashboard/new")
                        .param("from", "2026-06-01T00:00:00")
                        .param("to", "2026-05-01T00:00:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ADMIN_CUSTOMER_QUERY"));

        verify(getAdminNewCustomersUseCase, never()).execute(any());
    }

    private static AdminCustomerRecordResult recordResult() {
        return new AdminCustomerRecordResult(
                RECORD_ID,
                "CC",
                "100200300",
                "Ada",
                "Lovelace",
                NOW,
                NOW,
                List.of(accountResult(UserStatus.ACTIVE)));
    }

    private static AdminCustomerAccountResult accountResult(UserStatus status) {
        return new AdminCustomerAccountResult(
                USER_ID, "ada@example.com", "3001234567", status, NOW, null, RECORD_ID);
    }
}
