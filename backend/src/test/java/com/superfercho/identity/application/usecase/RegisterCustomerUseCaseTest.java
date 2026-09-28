package com.superfercho.identity.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.superfercho.identity.application.dto.RegisterCustomerCommand;
import com.superfercho.identity.application.dto.RegisteredCustomer;
import com.superfercho.identity.application.exception.DocumentAlreadyExistsException;
import com.superfercho.identity.application.exception.InvalidRegistrationException;
import com.superfercho.identity.application.exception.UserAlreadyExistsException;
import com.superfercho.identity.application.fakes.FakePasswordHasher;
import com.superfercho.identity.application.fakes.InMemoryCustomerRecordRepository;
import com.superfercho.identity.application.fakes.InMemoryUserRepository;
import com.superfercho.identity.domain.model.CustomerRecord;
import com.superfercho.identity.domain.model.Role;
import com.superfercho.identity.domain.model.User;
import com.superfercho.identity.domain.model.UserStatus;
import java.lang.reflect.RecordComponent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class RegisterCustomerUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-01-15T12:00:00Z");

    private InMemoryUserRepository users;
    private InMemoryCustomerRecordRepository customerRecords;
    private FakePasswordHasher passwordHasher;
    private RegisterCustomerUseCase useCase;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepository();
        customerRecords = new InMemoryCustomerRecordRepository();
        passwordHasher = new FakePasswordHasher();
        useCase = new RegisterCustomerUseCase(
                users, customerRecords, passwordHasher, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void shouldRegisterCustomerSuccessfully() {
        RegisteredCustomer result = useCase.execute(validCommand().build());

        assertNotNull(result.id());
        assertEquals("CC", result.documentType());
        assertEquals("12345678", result.documentNumber());
        assertEquals("Ada", result.firstName());
        assertEquals("Lovelace", result.lastName());
        assertEquals("ada@example.com", result.email());
        assertEquals("3001234567", result.phone());
        assertEquals(Role.CUSTOMER, result.role());
        assertEquals(UserStatus.ACTIVE, result.status());
        assertEquals(NOW, result.createdAt());

        User saved = users.findById(result.id()).orElseThrow();
        assertNotNull(saved.customerRecordId());
        assertNull(saved.deletedAt());
        CustomerRecord record = customerRecords.findById(saved.customerRecordId()).orElseThrow();
        assertEquals("CC", record.documentType());
        assertEquals("12345678", record.documentNumber());
        assertEquals("Ada", record.billingFirstName());
        assertEquals("Lovelace", record.billingLastName());
    }

    @Test
    void shouldNormalizeEmailDuringRegistration() {
        RegisteredCustomer result =
                useCase.execute(validCommand().email("  Ada.Lovelace@Example.COM  ").build());

        assertEquals("ada.lovelace@example.com", result.email());
        assertTrue(users.existsByEmail("ada.lovelace@example.com"));
    }

    @Test
    void shouldHashPasswordThroughThePort() {
        RegisteredCustomer result = useCase.execute(validCommand().password("Luis123!").build());

        assertEquals("Luis123!", passwordHasher.lastRawPassword());
        User saved = users.findById(result.id()).orElseThrow();
        assertEquals("hashed:Luis123!", saved.passwordHash());
    }

    @Test
    void shouldAssignCustomerRoleAndActiveStatus() {
        RegisteredCustomer result = useCase.execute(validCommand().build());

        User saved = users.findById(result.id()).orElseThrow();
        assertEquals(Role.CUSTOMER, saved.role());
        assertEquals(UserStatus.ACTIVE, saved.status());
    }

    @Test
    void shouldNotExposePasswordInRegistrationResult() {
        RegisteredCustomer result = useCase.execute(validCommand().build());

        assertTrue(Arrays.stream(RegisteredCustomer.class.getRecordComponents())
                .map(RecordComponent::getName)
                .noneMatch(name -> name.equals("password") || name.equals("passwordHash")));
        assertTrue(!result.toString().contains("Luis123!"));
        assertTrue(!result.toString().contains("hashed:"));
    }

    @Test
    void shouldRejectRegistrationWhenEmailAlreadyExists() {
        useCase.execute(validCommand().email("Ada@Example.com").build());

        assertThrows(
                UserAlreadyExistsException.class,
                () -> useCase.execute(
                        validCommand().email("ada@example.com").documentNumber("999").build()));
    }

    @Test
    void shouldRejectRegistrationWhenDocumentAlreadyHasActiveUser() {
        useCase.execute(validCommand().build());

        assertThrows(
                DocumentAlreadyExistsException.class,
                () -> useCase.execute(validCommand().email("other@example.com").build()));
        assertEquals(1, customerRecords.findByDocument("CC", "12345678").stream().count());
        assertEquals(1, users.findLiveByCustomerRecordId(
                        customerRecords.findByDocument("CC", "12345678").orElseThrow().id())
                .stream()
                .count());
    }

    @Test
    void shouldRejectRegistrationWhenDocumentAlreadyHasInactiveLiveUser() {
        RegisteredCustomer first = useCase.execute(validCommand().build());
        User live = users.findById(first.id()).orElseThrow();
        users.save(User.create(
                live.id(),
                live.documentType(),
                live.documentNumber(),
                live.firstName(),
                live.lastName(),
                live.email(),
                live.phone(),
                live.passwordHash(),
                live.role(),
                UserStatus.INACTIVE,
                live.customerRecordId(),
                null,
                live.createdAt(),
                NOW));

        assertThrows(
                DocumentAlreadyExistsException.class,
                () -> useCase.execute(validCommand().email("other@example.com").build()));
        assertEquals(
                first.id(),
                users.findLiveByCustomerRecordId(live.customerRecordId()).orElseThrow().id());
    }

    @Test
    void shouldReRegisterWhenOnlyDeletedUsersExistForCustomerRecord() {
        RegisteredCustomer first = useCase.execute(validCommand().build());
        User closed = users.findById(first.id()).orElseThrow();
        UUID recordId = closed.customerRecordId();
        CustomerRecord original = customerRecords.findById(recordId).orElseThrow();
        users.save(User.create(
                closed.id(),
                closed.documentType(),
                closed.documentNumber(),
                closed.firstName(),
                closed.lastName(),
                closed.email(),
                closed.phone(),
                closed.passwordHash(),
                closed.role(),
                UserStatus.INACTIVE,
                recordId,
                NOW,
                closed.createdAt(),
                NOW));

        RegisteredCustomer second = useCase.execute(validCommand()
                .email("ada.reopened@example.com")
                .phone("3009999999")
                .password("Nuevo123!")
                .firstName("Ada")
                .lastName("Reloaded")
                .build());

        assertNotEquals(first.id(), second.id());
        User reopened = users.findById(second.id()).orElseThrow();
        assertEquals(recordId, reopened.customerRecordId());
        assertEquals(UserStatus.ACTIVE, reopened.status());
        assertNull(reopened.deletedAt());
        assertEquals("ada.reopened@example.com", reopened.email());
        assertEquals("3009999999", reopened.phone());
        assertEquals("hashed:Nuevo123!", reopened.passwordHash());

        CustomerRecord unchanged = customerRecords.findById(recordId).orElseThrow();
        assertEquals(original.billingFirstName(), unchanged.billingFirstName());
        assertEquals(original.billingLastName(), unchanged.billingLastName());
        assertEquals(original.documentNumber(), unchanged.documentNumber());
        assertEquals(original.createdAt(), unchanged.createdAt());
    }

    @Test
    void shouldAllowReusingEmailFromDeletedUser() {
        RegisteredCustomer first = useCase.execute(validCommand().email("reuse@example.com").build());
        User closed = users.findById(first.id()).orElseThrow();
        users.save(User.create(
                closed.id(),
                closed.documentType(),
                closed.documentNumber(),
                closed.firstName(),
                closed.lastName(),
                closed.email(),
                closed.phone(),
                closed.passwordHash(),
                closed.role(),
                UserStatus.INACTIVE,
                closed.customerRecordId(),
                NOW,
                closed.createdAt(),
                NOW));

        RegisteredCustomer second = useCase.execute(validCommand()
                .email("reuse@example.com")
                .documentNumber("87654321")
                .build());

        assertEquals("reuse@example.com", second.email());
        assertNotEquals(first.id(), second.id());
        assertEquals("reuse@example.com", users.findById(first.id()).orElseThrow().email());
    }

    @Test
    void shouldNotInheritOperationalIdentityOfDeletedUserBeyondCommercialRecord() {
        RegisteredCustomer first = useCase.execute(validCommand().build());
        User closed = users.findById(first.id()).orElseThrow();
        users.save(User.create(
                closed.id(),
                closed.documentType(),
                closed.documentNumber(),
                closed.firstName(),
                closed.lastName(),
                closed.email(),
                closed.phone(),
                "hashed:OldPass1!",
                closed.role(),
                UserStatus.INACTIVE,
                closed.customerRecordId(),
                NOW,
                closed.createdAt(),
                NOW));

        RegisteredCustomer second =
                useCase.execute(validCommand().email("fresh@example.com").password("Fresh123!").build());

        User reopened = users.findById(second.id()).orElseThrow();
        assertNotEquals(closed.id(), reopened.id());
        assertNotEquals(closed.passwordHash(), reopened.passwordHash());
        assertNotEquals(closed.email(), reopened.email());
        assertEquals(closed.customerRecordId(), reopened.customerRecordId());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   ", "secret", "luis123!"})
    void shouldRejectRegistrationWhenPasswordIsInvalid(String password) {
        assertThrows(
                InvalidRegistrationException.class,
                () -> useCase.execute(validCommand().password(password).build()));
    }

    @Test
    void shouldRejectRegistrationWhenRequiredFieldsAreInvalid() {
        assertThrows(
                InvalidRegistrationException.class,
                () -> useCase.execute(validCommand().firstName("  ").build()));
        assertThrows(
                InvalidRegistrationException.class,
                () -> useCase.execute(validCommand().lastName("  ").build()));
        assertThrows(
                InvalidRegistrationException.class,
                () -> useCase.execute(validCommand().email(null).build()));
        assertThrows(
                InvalidRegistrationException.class,
                () -> useCase.execute(validCommand().email("   ").build()));
        assertThrows(
                InvalidRegistrationException.class,
                () -> useCase.execute(validCommand().phone("2001234567").build()));
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

        private CommandBuilder documentNumber(String documentNumber) {
            this.documentNumber = documentNumber;
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
