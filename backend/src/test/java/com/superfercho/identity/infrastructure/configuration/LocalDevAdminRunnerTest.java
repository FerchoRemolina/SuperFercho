package com.superfercho.identity.infrastructure.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.superfercho.identity.application.dto.AuthenticateUserCommand;
import com.superfercho.identity.application.dto.AuthenticationResult;
import com.superfercho.identity.application.fakes.FakePasswordHasher;
import com.superfercho.identity.application.fakes.InMemoryUserRepository;
import com.superfercho.identity.application.port.AccessTokenIssuer;
import com.superfercho.identity.application.port.IssuedAccessToken;
import com.superfercho.identity.application.port.UserRepository;
import com.superfercho.identity.application.usecase.AuthenticateUserUseCase;
import com.superfercho.identity.domain.model.Role;
import com.superfercho.identity.domain.model.User;
import com.superfercho.identity.domain.model.UserStatus;
import com.superfercho.identity.infrastructure.security.BCryptPasswordHasher;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class LocalDevAdminRunnerTest {

    private static final Instant NOW = Instant.parse("2026-09-21T12:00:00Z");
    private static final Instant CREATED_AT = Instant.parse("2026-01-15T12:00:00Z");
    private static final String EMAIL = "admin@localhost";
    private static final String PASSWORD = "local-dev-password";

    private CountingUserRepository users;
    private FakePasswordHasher passwordHasher;
    private LocalDevAdminRunner runner;

    @BeforeEach
    void setUp() {
        users = new CountingUserRepository();
        passwordHasher = new FakePasswordHasher();
        runner = new LocalDevAdminRunner(
                users, passwordHasher, Clock.fixed(NOW, ZoneOffset.UTC), EMAIL, PASSWORD);
    }

    @Test
    void isRestrictedToLocalProfile() {
        Profile profile = LocalDevAdminRunner.class.getAnnotation(Profile.class);

        assertNotNull(profile);
        assertEquals(1, profile.value().length);
        assertEquals("local", profile.value()[0]);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void doesNotInventCredentialsWhenEmailIsMissing(String email) {
        runner = new LocalDevAdminRunner(
                users, passwordHasher, Clock.fixed(NOW, ZoneOffset.UTC), email, PASSWORD);

        runner.ensureAdmin(email, PASSWORD);

        assertFalse(users.existsByEmail(EMAIL));
        assertEquals(0, users.saveCount());
        assertNull(passwordHasher.lastRawPassword());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void doesNotInventCredentialsWhenPasswordIsMissing(String password) {
        runner = new LocalDevAdminRunner(
                users, passwordHasher, Clock.fixed(NOW, ZoneOffset.UTC), EMAIL, password);

        runner.ensureAdmin(EMAIL, password);

        assertFalse(users.existsByEmail(EMAIL));
        assertEquals(0, users.saveCount());
        assertNull(passwordHasher.lastRawPassword());
    }

    @Test
    void createsActiveAdminWithHashedPasswordWhenConfigured() {
        runner.ensureAdmin(EMAIL, PASSWORD);

        User saved = users.findByEmail(EMAIL).orElseThrow();
        assertEquals(Role.ADMIN, saved.role());
        assertEquals(UserStatus.ACTIVE, saved.status());
        assertEquals(EMAIL, saved.email());
        assertEquals("hashed:" + PASSWORD, saved.passwordHash());
        assertEquals(PASSWORD, passwordHasher.lastRawPassword());
        assertEquals(LocalDevAdminRunner.DOCUMENT_TYPE, saved.documentType());
        assertEquals(saved.id().toString().replace("-", ""), saved.documentNumber());
        assertEquals(LocalDevAdminRunner.FULL_NAME, saved.fullName());
        assertEquals(LocalDevAdminRunner.PHONE, saved.phone());
        assertEquals(NOW, saved.createdAt());
        assertEquals(NOW, saved.updatedAt());
        assertEquals(1, users.saveCount());
        assertEquals(1, users.userCount());
        assertNotEquals(PASSWORD, saved.passwordHash());
    }

    @Test
    void hashesPasswordWithExistingBCryptHasher() {
        BCryptPasswordHasher bcrypt = new BCryptPasswordHasher(new BCryptPasswordEncoder());
        runner = new LocalDevAdminRunner(
                users, bcrypt, Clock.fixed(NOW, ZoneOffset.UTC), EMAIL, PASSWORD);

        runner.ensureAdmin(EMAIL, PASSWORD);

        User saved = users.findByEmail(EMAIL).orElseThrow();
        assertTrue(bcrypt.matches(PASSWORD, saved.passwordHash()));
        assertTrue(saved.passwordHash().startsWith("$2"));
        assertNotEquals(PASSWORD, saved.passwordHash());
    }

    @Test
    void normalizesConfiguredEmail() {
        runner.ensureAdmin("  Admin@LocalHost  ", PASSWORD);

        User saved = users.findByEmail(EMAIL).orElseThrow();
        assertEquals(EMAIL, saved.email());
    }

    @Test
    void secondRunDoesNotCreateAnotherUser() {
        runner.ensureAdmin(EMAIL, PASSWORD);
        User first = users.findByEmail(EMAIL).orElseThrow();
        UUID firstId = first.id();
        int savesAfterCreate = users.saveCount();

        runner.ensureAdmin(EMAIL, PASSWORD);

        User second = users.findByEmail(EMAIL).orElseThrow();
        assertEquals(firstId, second.id());
        assertEquals(savesAfterCreate, users.saveCount());
        assertEquals(1, users.userCount());
    }

    @Test
    void updatesExistingUserToConfiguredAdminWithoutChangingId() {
        UUID existingId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        users.save(user(
                existingId, EMAIL, Role.CUSTOMER, UserStatus.INACTIVE, "hashed:old-password"));

        runner.ensureAdmin(EMAIL, PASSWORD);

        User updated = users.findByEmail(EMAIL).orElseThrow();
        assertEquals(existingId, updated.id());
        assertEquals(Role.ADMIN, updated.role());
        assertEquals(UserStatus.ACTIVE, updated.status());
        assertEquals("hashed:" + PASSWORD, updated.passwordHash());
        assertEquals("Ada Lovelace", updated.fullName());
        assertEquals("12345678", updated.documentNumber());
        assertEquals(CREATED_AT, updated.createdAt());
        assertEquals(NOW, updated.updatedAt());
        assertEquals(1, users.userCount());
    }

    @Test
    void leavesOtherCustomersUnchanged() {
        UUID customerId = UUID.fromString("11111111-2222-3333-4444-555555555555");
        users.save(user(
                customerId,
                "customer@example.com",
                Role.CUSTOMER,
                UserStatus.ACTIVE,
                "hashed:customer-secret"));

        runner.ensureAdmin(EMAIL, PASSWORD);

        User customer = users.findById(customerId).orElseThrow();
        assertEquals(Role.CUSTOMER, customer.role());
        assertEquals(UserStatus.ACTIVE, customer.status());
        assertEquals("hashed:customer-secret", customer.passwordHash());
        assertEquals("customer@example.com", customer.email());

        User admin = users.findByEmail(EMAIL).orElseThrow();
        assertEquals(Role.ADMIN, admin.role());
        assertNotEquals(customerId, admin.id());
        assertEquals(2, users.userCount());
    }

    @Test
    void bootstrappedAdminAuthenticatesWithAdminRole() {
        runner.ensureAdmin(EMAIL, PASSWORD);

        AuthenticateUserUseCase authenticate =
                new AuthenticateUserUseCase(users, passwordHasher, new StubAccessTokenIssuer());
        AuthenticationResult result =
                authenticate.execute(new AuthenticateUserCommand(EMAIL, PASSWORD));

        assertEquals(users.findByEmail(EMAIL).orElseThrow().id(), result.userId());
        assertEquals(Role.ADMIN, result.role());
        assertTrue(result.accessToken().endsWith("-ADMIN"));
    }

    private static User user(UUID id, String email, Role role, UserStatus status, String passwordHash) {
        return User.create(
                id,
                "CC",
                "12345678",
                "Ada Lovelace",
                email,
                "3001234567",
                passwordHash,
                role,
                status,
                CREATED_AT,
                CREATED_AT);
    }

    private static final class CountingUserRepository implements UserRepository {

        private final InMemoryUserRepository delegate = new InMemoryUserRepository();
        private final Set<UUID> ids = new LinkedHashSet<>();
        private int saveCount;

        @Override
        public User save(User user) {
            saveCount++;
            ids.add(user.id());
            return delegate.save(user);
        }

        @Override
        public Optional<User> findById(UUID id) {
            return delegate.findById(id);
        }

        @Override
        public boolean existsByEmail(String email) {
            return delegate.existsByEmail(email);
        }

        @Override
        public boolean existsByDocument(String documentType, String documentNumber) {
            return delegate.existsByDocument(documentType, documentNumber);
        }

        @Override
        public Optional<User> findByEmail(String email) {
            return delegate.findByEmail(email);
        }

        int saveCount() {
            return saveCount;
        }

        int userCount() {
            return ids.size();
        }
    }

    private static final class StubAccessTokenIssuer implements AccessTokenIssuer {

        @Override
        public IssuedAccessToken issue(UUID userId, Role role) {
            return issue(userId, role, null);
        }

        @Override
        public IssuedAccessToken issue(UUID userId, Role role, UUID previewId) {
            return new IssuedAccessToken("token-" + userId + "-" + role.name(), NOW.plusSeconds(900));
        }
    }
}
