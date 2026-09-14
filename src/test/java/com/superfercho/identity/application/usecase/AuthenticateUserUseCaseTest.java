package com.superfercho.identity.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.superfercho.identity.application.dto.AuthenticateUserCommand;
import com.superfercho.identity.application.dto.AuthenticationResult;
import com.superfercho.identity.application.exception.InactiveUserException;
import com.superfercho.identity.application.exception.InvalidCredentialsException;
import com.superfercho.identity.application.fakes.FakePasswordHasher;
import com.superfercho.identity.application.fakes.InMemoryUserRepository;
import com.superfercho.identity.application.port.AccessTokenIssuer;
import com.superfercho.identity.application.port.IssuedAccessToken;
import com.superfercho.identity.domain.model.Role;
import com.superfercho.identity.domain.model.User;
import com.superfercho.identity.domain.model.UserStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthenticateUserUseCaseTest {

    private static final Instant EXPIRES_AT = Instant.parse("2026-01-15T12:15:00Z");
    private static final Instant CREATED_AT = Instant.parse("2026-01-15T12:00:00Z");
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private InMemoryUserRepository users;
    private FakePasswordHasher passwordHasher;
    private AuthenticateUserUseCase useCase;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepository();
        passwordHasher = new FakePasswordHasher();
        useCase = new AuthenticateUserUseCase(users, passwordHasher, new StubAccessTokenIssuer());
        users.save(user(USER_ID, "ada@example.com", UserStatus.ACTIVE, Role.CUSTOMER));
    }

    @Test
    void shouldAuthenticateExistingUserWithCorrectPassword() {
        AuthenticationResult result =
                useCase.execute(new AuthenticateUserCommand("ada@example.com", "secret"));

        assertEquals(USER_ID, result.userId());
        assertEquals(Role.CUSTOMER, result.role());
        assertEquals("token-" + USER_ID + "-CUSTOMER", result.accessToken());
        assertEquals(EXPIRES_AT, result.expiresAt());
        assertTrue(!result.toString().contains("hashed:"));
        assertTrue(!result.toString().contains("secret"));
    }

    @Test
    void shouldRejectAuthenticationWhenPasswordIsIncorrect() {
        assertThrows(
                InvalidCredentialsException.class,
                () -> useCase.execute(new AuthenticateUserCommand("ada@example.com", "wrong")));
    }

    @Test
    void shouldRejectAuthenticationWhenUserDoesNotExist() {
        assertThrows(
                InvalidCredentialsException.class,
                () -> useCase.execute(new AuthenticateUserCommand("missing@example.com", "secret")));
    }

    @Test
    void shouldRejectAuthenticationWhenUserIsInactive() {
        UUID inactiveId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        users.save(user(inactiveId, "inactive@example.com", UserStatus.INACTIVE, Role.CUSTOMER));

        assertThrows(
                InactiveUserException.class,
                () -> useCase.execute(new AuthenticateUserCommand("inactive@example.com", "secret")));
    }

    @Test
    void shouldAuthenticateWhenEmailDiffersOnlyByCase() {
        AuthenticationResult result =
                useCase.execute(new AuthenticateUserCommand("  Ada@Example.COM  ", "secret"));

        assertEquals(USER_ID, result.userId());
        assertEquals(Role.CUSTOMER, result.role());
    }

    private static User user(UUID id, String email, UserStatus status, Role role) {
        return User.create(
                id,
                "CC",
                id.toString().substring(0, 8),
                "Ada Lovelace",
                email,
                "3001234567",
                "hashed:secret",
                role,
                status,
                CREATED_AT,
                CREATED_AT);
    }

    private static final class StubAccessTokenIssuer implements AccessTokenIssuer {

        @Override
        public IssuedAccessToken issue(UUID userId, Role role) {
            return new IssuedAccessToken("token-" + userId + "-" + role.name(), EXPIRES_AT);
        }
    }
}
