package com.superfercho.identity.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.superfercho.identity.application.dto.RegisterCustomerCommand;
import com.superfercho.identity.application.dto.RegisteredCustomer;
import com.superfercho.identity.application.exception.DocumentAlreadyExistsException;
import com.superfercho.identity.application.exception.InvalidRegistrationException;
import com.superfercho.identity.application.exception.UserAlreadyExistsException;
import com.superfercho.identity.application.fakes.FakePasswordHasher;
import com.superfercho.identity.application.fakes.InMemoryUserRepository;
import com.superfercho.identity.domain.exception.InvalidUserException;
import com.superfercho.identity.domain.model.Role;
import com.superfercho.identity.domain.model.User;
import com.superfercho.identity.domain.model.UserStatus;
import java.lang.reflect.RecordComponent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class RegisterCustomerUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-01-15T12:00:00Z");

    private InMemoryUserRepository users;
    private FakePasswordHasher passwordHasher;
    private RegisterCustomerUseCase useCase;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepository();
        passwordHasher = new FakePasswordHasher();
        useCase = new RegisterCustomerUseCase(
                users, passwordHasher, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void shouldRegisterCustomerSuccessfully() {
        RegisteredCustomer result = useCase.execute(validCommand().build());

        assertNotNull(result.id());
        assertEquals("CC", result.documentType());
        assertEquals("12345678", result.documentNumber());
        assertEquals("Ada Lovelace", result.fullName());
        assertEquals("ada@example.com", result.email());
        assertEquals("3001234567", result.phone());
        assertEquals(Role.CUSTOMER, result.role());
        assertEquals(UserStatus.ACTIVE, result.status());
        assertEquals(NOW, result.createdAt());
    }

    @Test
    void shouldNormalizeEmailDuringRegistration() {
        RegisteredCustomer result = useCase.execute(validCommand().email("  Ada.Lovelace@Example.COM  ").build());

        assertEquals("ada.lovelace@example.com", result.email());
        assertTrue(users.existsByEmail("ada.lovelace@example.com"));
    }

    @Test
    void shouldHashPasswordThroughThePort() {
        RegisteredCustomer result = useCase.execute(validCommand().password("secret-pass").build());

        assertEquals("secret-pass", passwordHasher.lastRawPassword());
        User saved = users.findById(result.id()).orElseThrow();
        assertEquals("hashed:secret-pass", saved.passwordHash());
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
        assertTrue(!result.toString().contains("secret"));
        assertTrue(!result.toString().contains("hashed:"));
    }

    @Test
    void shouldRejectRegistrationWhenEmailAlreadyExists() {
        useCase.execute(validCommand().email("Ada@Example.com").build());

        assertThrows(
                UserAlreadyExistsException.class,
                () -> useCase.execute(validCommand().email("ada@example.com").documentNumber("999").build()));
    }

    @Test
    void shouldRejectRegistrationWhenDocumentAlreadyExists() {
        useCase.execute(validCommand().build());

        assertThrows(
                DocumentAlreadyExistsException.class,
                () -> useCase.execute(validCommand().email("other@example.com").build()));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void shouldRejectRegistrationWhenPasswordIsMissing(String password) {
        assertThrows(
                InvalidRegistrationException.class,
                () -> useCase.execute(validCommand().password(password).build()));
    }

    @Test
    void shouldRejectRegistrationWhenRequiredUserFieldIsMissing() {
        assertThrows(
                InvalidUserException.class, () -> useCase.execute(validCommand().fullName("  ").build()));
        assertThrows(InvalidUserException.class, () -> useCase.execute(validCommand().email(null).build()));
        assertThrows(
                InvalidUserException.class, () -> useCase.execute(validCommand().email("   ").build()));
    }

    private static CommandBuilder validCommand() {
        return new CommandBuilder();
    }

    private static final class CommandBuilder {
        private String documentType = "CC";
        private String documentNumber = "12345678";
        private String fullName = "Ada Lovelace";
        private String email = "ada@example.com";
        private String phone = "3001234567";
        private String password = "secret";

        private CommandBuilder email(String email) {
            this.email = email;
            return this;
        }

        private CommandBuilder documentNumber(String documentNumber) {
            this.documentNumber = documentNumber;
            return this;
        }

        private CommandBuilder fullName(String fullName) {
            this.fullName = fullName;
            return this;
        }

        private CommandBuilder password(String password) {
            this.password = password;
            return this;
        }

        private RegisterCustomerCommand build() {
            return new RegisterCustomerCommand(
                    documentType, documentNumber, fullName, email, phone, password);
        }
    }
}
