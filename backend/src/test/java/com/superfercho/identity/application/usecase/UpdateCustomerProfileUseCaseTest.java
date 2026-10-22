package com.superfercho.identity.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.superfercho.identity.application.dto.CustomerProfileResult;
import com.superfercho.identity.application.dto.RegisterCustomerCommand;
import com.superfercho.identity.application.dto.RegisteredCustomer;
import com.superfercho.identity.application.dto.UpdateCustomerProfileCommand;
import com.superfercho.identity.application.exception.CustomerProfileForbiddenException;
import com.superfercho.identity.application.exception.DocumentAlreadyExistsException;
import com.superfercho.identity.application.exception.InvalidRegistrationException;
import com.superfercho.identity.application.exception.UnauthenticatedUserException;
import com.superfercho.identity.application.exception.UserAlreadyExistsException;
import com.superfercho.identity.application.fakes.FakePasswordHasher;
import com.superfercho.identity.application.fakes.InMemoryCustomerPreviewRepository;
import com.superfercho.identity.application.fakes.InMemoryCustomerRecordRepository;
import com.superfercho.identity.application.fakes.InMemoryUserRepository;
import com.superfercho.identity.application.port.CurrentUserProvider;
import com.superfercho.identity.domain.model.CustomerPreview;
import com.superfercho.identity.domain.model.CustomerRecord;
import com.superfercho.identity.domain.model.Role;
import com.superfercho.identity.domain.model.User;
import com.superfercho.identity.domain.model.UserStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdateCustomerProfileUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-04-01T15:00:00Z");
    private static final Instant CREATED = Instant.parse("2026-01-15T12:00:00Z");

    @Mock
    private CurrentUserProvider currentUserProvider;

    private InMemoryUserRepository users;
    private InMemoryCustomerRecordRepository customerRecords;
    private InMemoryCustomerPreviewRepository previews;
    private UpdateCustomerProfileUseCase updateProfile;
    private RegisterCustomerUseCase registerCustomer;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepository();
        customerRecords = new InMemoryCustomerRecordRepository();
        previews = new InMemoryCustomerPreviewRepository();
        updateProfile = new UpdateCustomerProfileUseCase(
                currentUserProvider, users, customerRecords, Optional.of(previews), Clock.fixed(NOW, ZoneOffset.UTC));
        registerCustomer = new RegisterCustomerUseCase(
                users, customerRecords, new FakePasswordHasher(), Clock.fixed(CREATED, ZoneOffset.UTC));
    }

    @Test
    void shouldUpdateOwnProfileFields() {
        RegisteredCustomer registered = registerCustomer.execute(validRegistration().build());
        String passwordHashBefore = users.findById(registered.id()).orElseThrow().passwordHash();
        when(currentUserProvider.getCurrentUserId()).thenReturn(registered.id());

        CustomerProfileResult result = updateProfile.execute(validCommand()
                .firstName("Ada Clará")
                .lastName("Clará")
                .email("updated@example.com")
                .phone("3112223344")
                .buildUpdate());

        assertEquals("Ada Clará", result.firstName());
        assertEquals("Clará", result.lastName());
        assertEquals("updated@example.com", result.email());
        assertEquals("3112223344", result.phone());
        User saved = users.findById(registered.id()).orElseThrow();
        assertEquals("Ada Clará", saved.firstName());
        assertEquals("Clará", saved.lastName());
        assertEquals("updated@example.com", saved.email());
        assertEquals("3112223344", saved.phone());
        assertEquals(NOW, saved.updatedAt());
        assertEquals(passwordHashBefore, saved.passwordHash());
        assertEquals(Role.CUSTOMER, saved.role());
        assertEquals(UserStatus.ACTIVE, saved.status());
        // Documento sin cambio: el record comercial no se toca.
        CustomerRecord record = customerRecords.findById(saved.customerRecordId()).orElseThrow();
        assertEquals("CC", record.documentType());
        assertEquals("12345678", record.documentNumber());
        assertEquals(CREATED, record.updatedAt());
    }

    @Test
    void shouldNormalizeEmail() {
        registerCustomer.execute(validRegistration().build());
        when(currentUserProvider.getCurrentUserId())
                .thenReturn(users.findByEmail("ada@example.com").orElseThrow().id());

        CustomerProfileResult result = updateProfile.execute(
                validCommand().email("Ada.Updated@Example.COM").buildUpdate());

        assertEquals("ada.updated@example.com", result.email());
    }

    @Test
    void shouldAllowKeepingOwnEmail() {
        RegisteredCustomer registered = registerCustomer.execute(validRegistration().build());
        when(currentUserProvider.getCurrentUserId()).thenReturn(registered.id());

        CustomerProfileResult result =
                updateProfile.execute(validCommand().email("ada@example.com").buildUpdate());

        assertEquals("ada@example.com", result.email());
    }

    @Test
    void shouldRejectEmailOwnedByAnotherLiveUser() {
        registerCustomer.execute(validRegistration().build());
        registerCustomer.execute(validRegistration()
                .documentNumber("87654321")
                .email("other@example.com")
                .build());
        UUID firstUserId = users.findByEmail("ada@example.com").orElseThrow().id();
        when(currentUserProvider.getCurrentUserId()).thenReturn(firstUserId);

        assertThrows(
                UserAlreadyExistsException.class,
                () -> updateProfile.execute(validCommand().email("other@example.com").buildUpdate()));
        assertEquals("ada@example.com", users.findById(firstUserId).orElseThrow().email());
    }

    @Test
    void shouldUpdateDocumentConsistentlyAcrossUserAndCustomerRecord() {
        RegisteredCustomer registered = registerCustomer.execute(validRegistration().build());
        UUID recordId = users.findById(registered.id()).orElseThrow().customerRecordId();
        UUID stableUserId = registered.id();
        when(currentUserProvider.getCurrentUserId()).thenReturn(registered.id());

        updateProfile.execute(
                validCommand().documentType("CE").documentNumber("987654321").buildUpdate());

        User updated = users.findById(stableUserId).orElseThrow();
        assertEquals(stableUserId, updated.id());
        assertEquals(recordId, updated.customerRecordId());
        assertEquals("CE", updated.documentType());
        assertEquals("987654321", updated.documentNumber());
        CustomerRecord record = customerRecords.findById(recordId).orElseThrow();
        assertEquals("CE", record.documentType());
        assertEquals("987654321", record.documentNumber());
        assertEquals(NOW, record.updatedAt());
        // Los datos de facturación históricos no se reescriben.
        assertEquals("Ada", record.billingFirstName());
        assertEquals("Lovelace", record.billingLastName());
    }

    @Test
    void shouldRejectDocumentOwnedByAnotherCustomerRecord() {
        RegisteredCustomer first = registerCustomer.execute(validRegistration().build());
        registerCustomer.execute(validRegistration()
                .documentNumber("87654321")
                .email("other@example.com")
                .build());
        when(currentUserProvider.getCurrentUserId()).thenReturn(first.id());

        assertThrows(
                DocumentAlreadyExistsException.class,
                () -> updateProfile.execute(
                        validCommand().documentType("CC").documentNumber("87654321").buildUpdate()));
        User unchanged = users.findById(first.id()).orElseThrow();
        assertEquals("CC", unchanged.documentType());
        assertEquals("12345678", unchanged.documentNumber());
    }

    @Test
    void shouldRejectDocumentHeldByAnotherUserAccount() {
        registerCustomer.execute(validRegistration().build());
        // Cuenta con documento pero sin CustomerRecord propio (dato huérfano).
        UUID orphanId = UUID.randomUUID();
        users.save(User.create(
                orphanId,
                "CE",
                "987654321",
                "Other",
                "Person",
                "orphan@example.com",
                "3009999999",
                "hashed:pass",
                Role.CUSTOMER,
                UserStatus.ACTIVE,
                CREATED,
                CREATED));
        UUID firstUserId = users.findByEmail("ada@example.com").orElseThrow().id();
        when(currentUserProvider.getCurrentUserId()).thenReturn(firstUserId);

        assertThrows(
                DocumentAlreadyExistsException.class,
                () -> updateProfile.execute(
                        validCommand().documentType("CE").documentNumber("987654321").buildUpdate()));
        assertNotEquals(orphanId, firstUserId);
    }

    @Test
    void shouldRejectInvalidPhone() {
        RegisteredCustomer registered = registerCustomer.execute(validRegistration().build());
        when(currentUserProvider.getCurrentUserId()).thenReturn(registered.id());

        assertThrows(
                InvalidRegistrationException.class,
                () -> updateProfile.execute(validCommand().phone("123456").buildUpdate()));
    }

    @Test
    void shouldRejectBlankFirstName() {
        RegisteredCustomer registered = registerCustomer.execute(validRegistration().build());
        when(currentUserProvider.getCurrentUserId()).thenReturn(registered.id());

        assertThrows(
                InvalidRegistrationException.class,
                () -> updateProfile.execute(validCommand().firstName("   ").buildUpdate()));
    }

    @Test
    void shouldRejectInvalidDocumentType() {
        RegisteredCustomer registered = registerCustomer.execute(validRegistration().build());
        when(currentUserProvider.getCurrentUserId()).thenReturn(registered.id());

        assertThrows(
                InvalidRegistrationException.class,
                () -> updateProfile.execute(validCommand().documentType("TI").buildUpdate()));
    }

    @Test
    void shouldRejectAdminAccount() {
        users.save(User.create(
                UUID.randomUUID(),
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
        UUID adminId = users.findByEmail("admin@example.com").orElseThrow().id();
        when(currentUserProvider.getCurrentUserId()).thenReturn(adminId);

        assertThrows(
                CustomerProfileForbiddenException.class, () -> updateProfile.execute(validCommand().buildUpdate()));
    }

    @Test
    void shouldRejectClosedAccount() {
        RegisteredCustomer registered = registerCustomer.execute(validRegistration().build());
        User closed = users.findById(registered.id()).orElseThrow().closeAccount(NOW);
        users.save(closed);
        when(currentUserProvider.getCurrentUserId()).thenReturn(registered.id());

        assertThrows(
                CustomerProfileForbiddenException.class, () -> updateProfile.execute(validCommand().buildUpdate()));
    }

    @Test
    void shouldRejectPreviewTemporaryCustomerTrackedInPreviewRepository() {
        RegisteredCustomer registered = registerCustomer.execute(validRegistration().build());
        previews.save(CustomerPreview.start(UUID.randomUUID(), UUID.randomUUID(), registered.id(), CREATED));
        when(currentUserProvider.getCurrentUserId()).thenReturn(registered.id());

        assertThrows(
                CustomerProfileForbiddenException.class, () -> updateProfile.execute(validCommand().buildUpdate()));
    }

    @Test
    void shouldPropagateUnauthenticatedFromCurrentUserProvider() {
        when(currentUserProvider.getCurrentUserId()).thenThrow(new UnauthenticatedUserException());

        assertThrows(UnauthenticatedUserException.class, () -> updateProfile.execute(validCommand().buildUpdate()));
    }

    private static CommandBuilder validRegistration() {
        return new CommandBuilder();
    }

    private static CommandBuilder validCommand() {
        return new CommandBuilder();
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

        private CommandBuilder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        private CommandBuilder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        private CommandBuilder phone(String phone) {
            this.phone = phone;
            return this;
        }

        private CommandBuilder documentType(String documentType) {
            this.documentType = documentType;
            return this;
        }

        private CommandBuilder documentNumber(String documentNumber) {
            this.documentNumber = documentNumber;
            return this;
        }

        private RegisterCustomerCommand build() {
            return new RegisterCustomerCommand(
                    documentType, documentNumber, firstName, lastName, email, phone, password);
        }

        private UpdateCustomerProfileCommand buildUpdate() {
            return new UpdateCustomerProfileCommand(firstName, lastName, email, phone, documentType, documentNumber);
        }
    }
}
