package com.superfercho.identity.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.superfercho.identity.application.dto.RegisterCustomerCommand;
import com.superfercho.identity.application.dto.RegisteredCustomer;
import com.superfercho.identity.application.exception.CloseCustomerAccountForbiddenException;
import com.superfercho.identity.application.exception.UnauthenticatedUserException;
import com.superfercho.identity.application.fakes.FakePasswordHasher;
import com.superfercho.identity.application.fakes.InMemoryAddressRepository;
import com.superfercho.identity.application.fakes.InMemoryCustomerPreviewRepository;
import com.superfercho.identity.application.fakes.InMemoryCustomerRecordRepository;
import com.superfercho.identity.application.fakes.InMemoryPasswordRecoveryTokenRepository;
import com.superfercho.identity.application.fakes.InMemoryUserRepository;
import com.superfercho.identity.application.port.CurrentUserProvider;
import com.superfercho.identity.application.port.CustomerAccountAssistantCleanupPort;
import com.superfercho.identity.application.port.CustomerAccountCheckoutCleanupPort;
import com.superfercho.identity.application.port.CustomerAccountShoppingCleanupPort;
import com.superfercho.identity.domain.model.Address;
import com.superfercho.identity.domain.model.AddressStatus;
import com.superfercho.identity.domain.model.CustomerPreview;
import com.superfercho.identity.domain.model.CustomerRecord;
import com.superfercho.identity.domain.model.PasswordRecoveryToken;
import com.superfercho.identity.domain.model.Role;
import com.superfercho.identity.domain.model.User;
import com.superfercho.identity.domain.model.UserStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CloseCustomerAccountUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-04-01T15:00:00Z");
    private static final Instant CREATED = Instant.parse("2026-01-15T12:00:00Z");

    @Mock
    private CurrentUserProvider currentUserProvider;

    private InMemoryUserRepository users;
    private InMemoryCustomerRecordRepository customerRecords;
    private InMemoryAddressRepository addresses;
    private InMemoryPasswordRecoveryTokenRepository recoveryTokens;
    private InMemoryCustomerPreviewRepository previews;
    private RecordingShoppingCleanup shoppingCleanup;
    private RecordingCheckoutCleanup checkoutCleanup;
    private RecordingAssistantCleanup assistantCleanup;
    private CloseCustomerAccountUseCase closeAccount;
    private RegisterCustomerUseCase registerCustomer;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepository();
        customerRecords = new InMemoryCustomerRecordRepository();
        addresses = new InMemoryAddressRepository();
        recoveryTokens = new InMemoryPasswordRecoveryTokenRepository();
        previews = new InMemoryCustomerPreviewRepository();
        shoppingCleanup = new RecordingShoppingCleanup();
        checkoutCleanup = new RecordingCheckoutCleanup();
        assistantCleanup = new RecordingAssistantCleanup();
        closeAccount = new CloseCustomerAccountUseCase(
                currentUserProvider,
                users,
                addresses,
                recoveryTokens,
                shoppingCleanup,
                checkoutCleanup,
                assistantCleanup,
                previews,
                Clock.fixed(NOW, ZoneOffset.UTC));
        registerCustomer = new RegisterCustomerUseCase(
                users, customerRecords, new FakePasswordHasher(), Clock.fixed(CREATED, ZoneOffset.UTC));
    }

    @Test
    void shouldCloseActiveCustomerAccountAndClearOperationalData() {
        RegisteredCustomer registered = registerCustomer.execute(validRegistration().build());
        when(currentUserProvider.getCurrentUserId()).thenReturn(registered.id());
        UUID recordId = users.findById(registered.id()).orElseThrow().customerRecordId();
        addresses.save(registered.id(), address("Casa"));
        recoveryTokens.save(token(registered.id(), "hash-a"));
        CustomerRecord before = customerRecords.findById(recordId).orElseThrow();

        closeAccount.execute();

        User closed = users.findById(registered.id()).orElseThrow();
        assertEquals(UserStatus.INACTIVE, closed.status());
        assertEquals(NOW, closed.deletedAt());
        assertEquals(recordId, closed.customerRecordId());
        assertEquals(registered.email(), closed.email());
        assertEquals(registered.documentNumber(), closed.documentNumber());
        assertTrue(addresses.findByUserId(registered.id()).isEmpty());
        assertTrue(recoveryTokens.all().isEmpty());
        assertEquals(List.of(registered.id()), shoppingCleanup.deletedCustomerIds);
        assertEquals(List.of(registered.id()), checkoutCleanup.deletedCustomerIds);
        assertEquals(List.of(registered.id()), assistantCleanup.deletedUserIds);

        CustomerRecord after = customerRecords.findById(recordId).orElseThrow();
        assertEquals(before.billingFirstName(), after.billingFirstName());
        assertEquals(before.billingLastName(), after.billingLastName());
        assertEquals(before.documentNumber(), after.documentNumber());
        assertEquals(before.createdAt(), after.createdAt());
    }

    @Test
    void shouldNotTouchOtherUserOperationalData() {
        RegisteredCustomer owner = registerCustomer.execute(validRegistration().build());
        RegisteredCustomer other = registerCustomer.execute(validRegistration()
                .email("other@example.com")
                .documentNumber("87654321")
                .build());
        when(currentUserProvider.getCurrentUserId()).thenReturn(owner.id());
        addresses.save(owner.id(), address("Owner"));
        addresses.save(other.id(), address("Other"));
        recoveryTokens.save(token(owner.id(), "owner-hash"));
        recoveryTokens.save(token(other.id(), "other-hash"));

        closeAccount.execute();

        assertTrue(addresses.findByUserId(owner.id()).isEmpty());
        assertEquals(1, addresses.findByUserId(other.id()).size());
        assertEquals(1, recoveryTokens.all().size());
        assertEquals(other.id(), recoveryTokens.all().getFirst().userId());
        assertEquals(UserStatus.ACTIVE, users.findById(other.id()).orElseThrow().status());
        assertNull(users.findById(other.id()).orElseThrow().deletedAt());
    }

    @Test
    void shouldBeIdempotentWhenAccountAlreadyClosed() {
        RegisteredCustomer registered = registerCustomer.execute(validRegistration().build());
        when(currentUserProvider.getCurrentUserId()).thenReturn(registered.id());
        closeAccount.execute();
        shoppingCleanup.deletedCustomerIds.clear();
        checkoutCleanup.deletedCustomerIds.clear();
        assistantCleanup.deletedUserIds.clear();
        addresses.save(registered.id(), address("ShouldStayBecauseAlreadyClosedPathSkips"));

        closeAccount.execute();

        User closed = users.findById(registered.id()).orElseThrow();
        assertEquals(NOW, closed.deletedAt());
        assertEquals(UserStatus.INACTIVE, closed.status());
        assertTrue(shoppingCleanup.deletedCustomerIds.isEmpty());
        assertTrue(checkoutCleanup.deletedCustomerIds.isEmpty());
        assertTrue(assistantCleanup.deletedUserIds.isEmpty());
        assertEquals(1, addresses.findByUserId(registered.id()).size());
    }

    @Test
    void shouldRejectAdmin() {
        UUID adminId = UUID.randomUUID();
        users.save(User.create(
                adminId,
                "CC",
                "90000001",
                "Admin",
                "Local",
                "admin@example.com",
                "3000000000",
                "hashed:pass",
                Role.ADMIN,
                UserStatus.ACTIVE,
                CREATED,
                CREATED));
        when(currentUserProvider.getCurrentUserId()).thenReturn(adminId);

        assertThrows(CloseCustomerAccountForbiddenException.class, closeAccount::execute);
        assertTrue(shoppingCleanup.deletedCustomerIds.isEmpty());
    }

    @Test
    void shouldRejectPreviewTemporaryCustomerWithoutCustomerRecord() {
        UUID previewUserId = UUID.randomUUID();
        users.save(User.create(
                previewUserId,
                "CC",
                "PREV" + previewUserId.toString().replace("-", "").substring(0, 12),
                "Preview",
                "Temp",
                "preview+" + previewUserId + "@temp.superfercho.local",
                "3001111111",
                "hashed:pass",
                Role.CUSTOMER,
                UserStatus.ACTIVE,
                CREATED,
                CREATED));
        when(currentUserProvider.getCurrentUserId()).thenReturn(previewUserId);

        assertThrows(CloseCustomerAccountForbiddenException.class, closeAccount::execute);
    }

    @Test
    void shouldRejectPreviewTemporaryCustomerTrackedInPreviewRepository() {
        RegisteredCustomer registered = registerCustomer.execute(validRegistration().build());
        previews.save(CustomerPreview.start(
                UUID.randomUUID(), UUID.randomUUID(), registered.id(), CREATED));
        when(currentUserProvider.getCurrentUserId()).thenReturn(registered.id());

        assertThrows(CloseCustomerAccountForbiddenException.class, closeAccount::execute);
        assertNull(users.findById(registered.id()).orElseThrow().deletedAt());
    }

    @Test
    void shouldAllowReRegistrationAfterCloseReusingCustomerRecord() {
        RegisteredCustomer first = registerCustomer.execute(validRegistration().build());
        UUID recordId = users.findById(first.id()).orElseThrow().customerRecordId();
        when(currentUserProvider.getCurrentUserId()).thenReturn(first.id());
        closeAccount.execute();

        RegisteredCustomer second = registerCustomer.execute(validRegistration()
                .email("ada.reopened@example.com")
                .password("Nuevo123!")
                .build());

        assertNotEquals(first.id(), second.id());
        User reopened = users.findById(second.id()).orElseThrow();
        assertEquals(recordId, reopened.customerRecordId());
        assertEquals(UserStatus.ACTIVE, reopened.status());
        assertNull(reopened.deletedAt());
        assertEquals("ada.reopened@example.com", reopened.email());
        assertNotEquals(users.findById(first.id()).orElseThrow().passwordHash(), reopened.passwordHash());
        assertTrue(addresses.findByUserId(second.id()).isEmpty());
    }

    @Test
    void shouldPropagateUnauthenticatedFromCurrentUserProvider() {
        when(currentUserProvider.getCurrentUserId()).thenThrow(new UnauthenticatedUserException());

        assertThrows(UnauthenticatedUserException.class, closeAccount::execute);
        verify(currentUserProvider, times(1)).getCurrentUserId();
    }

    private static CommandBuilder validRegistration() {
        return new CommandBuilder();
    }

    private static Address address(String label) {
        return Address.create(
                UUID.randomUUID(),
                label,
                "Ada Lovelace",
                "Calle 1 # 2-3",
                null,
                "Bogotá",
                "Cundinamarca",
                "3001234567",
                true,
                AddressStatus.ACTIVE,
                CREATED,
                CREATED);
    }

    private static PasswordRecoveryToken token(UUID userId, String hash) {
        return PasswordRecoveryToken.create(
                UUID.randomUUID(), userId, hash, "127.0.0.1", CREATED, CREATED.plusSeconds(900));
    }

    private static final class RecordingShoppingCleanup implements CustomerAccountShoppingCleanupPort {
        private final List<UUID> deletedCustomerIds = new ArrayList<>();

        @Override
        public void deleteAllForCustomer(UUID customerId) {
            deletedCustomerIds.add(customerId);
        }
    }

    private static final class RecordingCheckoutCleanup implements CustomerAccountCheckoutCleanupPort {
        private final List<UUID> deletedCustomerIds = new ArrayList<>();

        @Override
        public void deleteCheckoutIdempotencyForCustomer(UUID customerId) {
            deletedCustomerIds.add(customerId);
        }
    }

    private static final class RecordingAssistantCleanup implements CustomerAccountAssistantCleanupPort {
        private final List<UUID> deletedUserIds = new ArrayList<>();

        @Override
        public void deleteAllForUser(UUID userId) {
            deletedUserIds.add(userId);
        }
    }

    private static final class CommandBuilder {
        private String documentType = "CC";
        private String documentNumber = "12345678";
        private String firstName = "Ada";
        private String lastName = "Lovelace";
        private String email = "ada@example.com";
        private String phone = "3001234567";
        private String password = "Luis123!";

        private CommandBuilder email(String email) {
            this.email = email;
            return this;
        }

        private CommandBuilder documentNumber(String documentNumber) {
            this.documentNumber = documentNumber;
            return this;
        }

        private CommandBuilder password(String password) {
            this.password = password;
            return this;
        }

        private RegisterCustomerCommand build() {
            return new RegisterCustomerCommand(
                    documentType, documentNumber, firstName, lastName, email, phone, password);
        }
    }
}
