package com.superfercho.identity.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.superfercho.identity.application.dto.ActivateAdminCustomerAccountCommand;
import com.superfercho.identity.application.dto.AdminAccountStatusFilter;
import com.superfercho.identity.application.dto.AdminCustomerAccountResult;
import com.superfercho.identity.application.dto.AdminCustomerRecordResult;
import com.superfercho.identity.application.dto.AdminPagedResult;
import com.superfercho.identity.application.dto.CustomerCommercialOrderView;
import com.superfercho.identity.application.dto.CustomerCommercialPaymentView;
import com.superfercho.identity.application.dto.DeactivateAdminCustomerAccountCommand;
import com.superfercho.identity.application.dto.FindAdminCustomerByDocumentCommand;
import com.superfercho.identity.application.dto.GetAdminCustomerRecordCommand;
import com.superfercho.identity.application.dto.ListAdminCustomerCommercialHistoryCommand;
import com.superfercho.identity.application.exception.CustomerRecordNotFoundException;
import com.superfercho.identity.application.exception.UserNotFoundException;
import com.superfercho.identity.application.fakes.InMemoryCustomerRecordRepository;
import com.superfercho.identity.application.fakes.InMemoryUserRepository;
import com.superfercho.identity.application.port.CustomerCommercialHistoryPort;
import com.superfercho.identity.domain.exception.InvalidUserException;
import com.superfercho.identity.domain.model.CustomerRecord;
import com.superfercho.identity.domain.model.Role;
import com.superfercho.identity.domain.model.User;
import com.superfercho.identity.domain.model.UserStatus;
import com.superfercho.platform.money.Money;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AdminCustomerAdministrationUseCasesTest {

    private static final Instant NOW = Instant.parse("2026-04-01T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final UUID RECORD_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID LIVE_ACTIVE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID LIVE_INACTIVE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID DELETED_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID ADMIN_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID PREVIEW_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID ORDER_A = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final UUID ORDER_B = UUID.fromString("77777777-7777-7777-7777-777777777777");
    private static final UUID PAYMENT_A = UUID.fromString("88888888-8888-8888-8888-888888888888");
    private static final UUID PAYMENT_B = UUID.fromString("99999999-9999-9999-9999-999999999999");

    private InMemoryCustomerRecordRepository customerRecords;
    private InMemoryUserRepository users;
    private FakeCommercialHistoryPort commercialHistory;
    private FindAdminCustomerByDocumentUseCase findByDocument;
    private GetAdminCustomerRecordUseCase getRecord;
    private ActivateAdminCustomerAccountUseCase activate;
    private DeactivateAdminCustomerAccountUseCase deactivate;
    private ListAdminCustomerOrdersUseCase listOrders;
    private ListAdminCustomerPaymentsUseCase listPayments;

    @BeforeEach
    void setUp() {
        customerRecords = new InMemoryCustomerRecordRepository();
        users = new InMemoryUserRepository();
        commercialHistory = new FakeCommercialHistoryPort();
        findByDocument = new FindAdminCustomerByDocumentUseCase(customerRecords, users);
        getRecord = new GetAdminCustomerRecordUseCase(customerRecords, users);
        activate = new ActivateAdminCustomerAccountUseCase(customerRecords, users, CLOCK);
        deactivate = new DeactivateAdminCustomerAccountUseCase(customerRecords, users, CLOCK);
        listOrders = new ListAdminCustomerOrdersUseCase(customerRecords, users, commercialHistory);
        listPayments = new ListAdminCustomerPaymentsUseCase(customerRecords, users, commercialHistory);

        customerRecords.save(CustomerRecord.create(
                RECORD_ID, "CC", "100200300", "Ada", "Lovelace", NOW.minusSeconds(10_000), NOW.minusSeconds(10_000)));
        users.save(customer(LIVE_ACTIVE_ID, "live.active@example.com", UserStatus.ACTIVE, null, NOW.minusSeconds(100)));
        users.save(customer(
                LIVE_INACTIVE_ID, "live.inactive@example.com", UserStatus.INACTIVE, null, NOW.minusSeconds(200)));
        users.save(customer(
                DELETED_ID,
                "deleted@example.com",
                UserStatus.INACTIVE,
                NOW.minusSeconds(50),
                NOW.minusSeconds(300)));
        users.save(User.create(
                ADMIN_ID,
                "CC",
                "99999999",
                "Admin",
                "Local",
                "admin@example.com",
                "3000000000",
                "hash",
                Role.ADMIN,
                UserStatus.ACTIVE,
                NOW,
                NOW));
        users.save(User.create(
                PREVIEW_ID,
                "CC",
                "88888888",
                "Preview",
                "Temp",
                "preview@example.com",
                "3000000001",
                "hash",
                Role.CUSTOMER,
                UserStatus.ACTIVE,
                NOW,
                NOW));
    }

    @Test
    void adminCanFindCustomerRecordByDocument() {
        AdminCustomerRecordResult result =
                findByDocument.execute(FindAdminCustomerByDocumentCommand.of("CC", "100200300", "ALL"));

        assertEquals(RECORD_ID, result.id());
        assertEquals("CC", result.documentType());
        assertEquals("100200300", result.documentNumber());
        assertEquals(3, result.accounts().size());
    }

    @Test
    void missingDocumentReturnsNotFoundAndCreatesNothing() {
        int before = customerRecords.findById(RECORD_ID).isPresent() ? 1 : 0;
        assertThrows(
                CustomerRecordNotFoundException.class,
                () -> findByDocument.execute(FindAdminCustomerByDocumentCommand.of("CC", "00000000", null)));
        assertEquals(before, customerRecords.findById(RECORD_ID).isPresent() ? 1 : 0);
        assertTrue(customerRecords.findByDocument("CC", "00000000").isEmpty());
    }

    @Test
    void customerRecordWithSingleLiveAccount() {
        UUID onlyRecord = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        UUID onlyUser = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        customerRecords.save(CustomerRecord.create(
                onlyRecord, "CC", "55555555", "Grace", "Hopper", NOW, NOW));
        users.save(User.create(
                onlyUser,
                "CC",
                "55555555",
                "Grace",
                "Hopper",
                "grace@example.com",
                "3001111111",
                "hash",
                Role.CUSTOMER,
                UserStatus.ACTIVE,
                onlyRecord,
                null,
                NOW,
                NOW));

        AdminCustomerRecordResult result =
                findByDocument.execute(FindAdminCustomerByDocumentCommand.of("CC", "55555555", "ACTIVE"));

        assertEquals(1, result.accounts().size());
        assertEquals(onlyUser, result.accounts().getFirst().id());
        assertEquals(UserStatus.ACTIVE, result.accounts().getFirst().status());
        assertEquals(null, result.accounts().getFirst().deletedAt());
    }

    @Test
    void filtersActiveInactiveDeletedAndAll() {
        assertEquals(
                List.of(LIVE_ACTIVE_ID),
                accountIds(getRecord.execute(GetAdminCustomerRecordCommand.of(RECORD_ID, "ACTIVE"))));
        assertEquals(
                List.of(LIVE_INACTIVE_ID),
                accountIds(getRecord.execute(GetAdminCustomerRecordCommand.of(RECORD_ID, "INACTIVE"))));
        assertEquals(
                List.of(DELETED_ID),
                accountIds(getRecord.execute(GetAdminCustomerRecordCommand.of(RECORD_ID, "DELETED"))));
        assertEquals(
                3, getRecord.execute(GetAdminCustomerRecordCommand.of(RECORD_ID, "ALL")).accounts().size());
    }

    @Test
    void deletedAccountNeverAppearsAsActive() {
        List<AdminCustomerAccountResult> active =
                getRecord.execute(GetAdminCustomerRecordCommand.of(RECORD_ID, "ACTIVE")).accounts();
        assertTrue(active.stream().noneMatch(account -> account.id().equals(DELETED_ID)));
        assertTrue(active.stream().allMatch(account -> account.deletedAt() == null));
        assertTrue(active.stream().allMatch(account -> account.status() == UserStatus.ACTIVE));
    }

    @Test
    void adminCanActivateLiveInactiveAccount() {
        AdminCustomerAccountResult result = activate.execute(
                new ActivateAdminCustomerAccountCommand(RECORD_ID, LIVE_INACTIVE_ID));

        assertEquals(UserStatus.ACTIVE, result.status());
        assertEquals(null, result.deletedAt());
        assertEquals(UserStatus.ACTIVE, users.findById(LIVE_INACTIVE_ID).orElseThrow().status());
    }

    @Test
    void adminCanDeactivateLiveActiveAccount() {
        AdminCustomerAccountResult result =
                deactivate.execute(new DeactivateAdminCustomerAccountCommand(RECORD_ID, LIVE_ACTIVE_ID));

        assertEquals(UserStatus.INACTIVE, result.status());
        assertEquals(null, result.deletedAt());
    }

    @Test
    void deletedAccountCannotBeReactivated() {
        assertThrows(
                InvalidUserException.class,
                () -> activate.execute(new ActivateAdminCustomerAccountCommand(RECORD_ID, DELETED_ID)));
        assertEquals(NOW.minusSeconds(50), users.findById(DELETED_ID).orElseThrow().deletedAt());
    }

    @Test
    void orderHistoryIncludesOrdersFromMultipleUsersOfSameRecord() {
        commercialHistory.orders.add(orderView(ORDER_A, LIVE_ACTIVE_ID, PAYMENT_A));
        commercialHistory.orders.add(orderView(ORDER_B, DELETED_ID, PAYMENT_B));

        AdminPagedResult<CustomerCommercialOrderView> page =
                listOrders.execute(new ListAdminCustomerCommercialHistoryCommand(RECORD_ID, 0, 20));

        assertEquals(2, page.totalElements());
        assertEquals(
                List.of(LIVE_ACTIVE_ID, DELETED_ID, LIVE_INACTIVE_ID).stream().sorted().toList(),
                commercialHistory.lastRequestedCustomerIds.stream().sorted().toList());
        assertEquals(List.of(ORDER_A, ORDER_B), page.items().stream().map(CustomerCommercialOrderView::id).toList());
    }

    @Test
    void paymentHistoryCorrespondsToFoundOrders() {
        commercialHistory.payments.add(paymentView(PAYMENT_A, ORDER_A));
        commercialHistory.payments.add(paymentView(PAYMENT_B, ORDER_B));

        AdminPagedResult<CustomerCommercialPaymentView> page =
                listPayments.execute(new ListAdminCustomerCommercialHistoryCommand(RECORD_ID, 0, 20));

        assertEquals(2, page.items().size());
        assertEquals(ORDER_A, page.items().get(0).orderId());
        assertEquals(ORDER_B, page.items().get(1).orderId());
        assertEquals(PAYMENT_A, page.items().get(0).id());
        assertEquals(PAYMENT_B, page.items().get(1).id());
    }

    @Test
    void accountResultsDoNotExposePasswordHash() {
        AdminCustomerAccountResult account =
                getRecord.execute(GetAdminCustomerRecordCommand.of(RECORD_ID, "ALL")).accounts().getFirst();
        String jsonLike = account.toString();
        assertTrue(!jsonLike.toLowerCase().contains("password"));
        assertTrue(!jsonLike.contains("hash"));
    }

    @Test
    void adminAndPreviewUsersAreNotLinkedToCustomerRecordAccounts() {
        AdminCustomerRecordResult result =
                getRecord.execute(GetAdminCustomerRecordCommand.of(RECORD_ID, AdminAccountStatusFilter.ALL.name()));
        assertTrue(result.accounts().stream().noneMatch(account -> account.id().equals(ADMIN_ID)));
        assertTrue(result.accounts().stream().noneMatch(account -> account.id().equals(PREVIEW_ID)));
    }

    @Test
    void activateRejectsUserOutsideCustomerRecord() {
        assertThrows(
                UserNotFoundException.class,
                () -> activate.execute(new ActivateAdminCustomerAccountCommand(RECORD_ID, ADMIN_ID)));
    }

    private User customer(
            UUID id, String email, UserStatus status, Instant deletedAt, Instant createdAt) {
        return User.create(
                id,
                "CC",
                "100200300",
                "Ada",
                "Lovelace",
                email,
                "3001234567",
                "secret-hash-value",
                Role.CUSTOMER,
                status,
                RECORD_ID,
                deletedAt,
                createdAt,
                createdAt);
    }

    private static List<UUID> accountIds(AdminCustomerRecordResult result) {
        return result.accounts().stream().map(AdminCustomerAccountResult::id).toList();
    }

    private static CustomerCommercialOrderView orderView(UUID orderId, UUID customerId, UUID paymentId) {
        Money amount = Money.cop(new BigDecimal("10000"));
        return new CustomerCommercialOrderView(
                orderId, "ORD-" + orderId, customerId, "CONFIRMED", amount, amount, paymentId, NOW, NOW, null, NOW);
    }

    private static CustomerCommercialPaymentView paymentView(UUID paymentId, UUID orderId) {
        return new CustomerCommercialPaymentView(
                paymentId,
                orderId,
                Money.cop(new BigDecimal("10000")),
                "SIMULATED_CARD",
                "APPROVED",
                "ref",
                NOW,
                NOW,
                null);
    }

    private static final class FakeCommercialHistoryPort implements CustomerCommercialHistoryPort {
        private final List<CustomerCommercialOrderView> orders = new ArrayList<>();
        private final List<CustomerCommercialPaymentView> payments = new ArrayList<>();
        private Collection<UUID> lastRequestedCustomerIds = List.of();

        @Override
        public AdminPagedResult<CustomerCommercialOrderView> findOrdersByCustomerIds(
                Collection<UUID> customerIds, Integer page, Integer size) {
            lastRequestedCustomerIds = List.copyOf(customerIds);
            return new AdminPagedResult<>(orders, 0, 20, orders.size());
        }

        @Override
        public AdminPagedResult<CustomerCommercialPaymentView> findPaymentsByCustomerIds(
                Collection<UUID> customerIds, Integer page, Integer size) {
            lastRequestedCustomerIds = List.copyOf(customerIds);
            return new AdminPagedResult<>(payments, 0, 20, payments.size());
        }
    }
}
