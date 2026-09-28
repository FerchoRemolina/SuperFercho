package com.superfercho.identity.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.superfercho.identity.application.dto.PasswordRecoveryRequestResult;
import com.superfercho.identity.application.dto.RequestPasswordRecoveryCommand;
import com.superfercho.identity.application.dto.ResetPasswordCommand;
import com.superfercho.identity.application.exception.InvalidPasswordRecoveryException;
import com.superfercho.identity.application.exception.InvalidRegistrationException;
import com.superfercho.identity.application.exception.PasswordRecoveryRateLimitedException;
import com.superfercho.identity.application.fakes.InMemoryPasswordRecoveryTokenRepository;
import com.superfercho.identity.application.fakes.InMemoryUserRepository;
import com.superfercho.identity.application.port.EmailMessage;
import com.superfercho.identity.application.port.EmailSender;
import com.superfercho.identity.application.port.PasswordHasher;
import com.superfercho.identity.application.port.PasswordRecoveryAbuseGuard;
import com.superfercho.identity.application.port.SecureTokenGenerator;
import com.superfercho.identity.domain.model.PasswordRecoveryToken;
import com.superfercho.identity.domain.model.Role;
import com.superfercho.identity.domain.model.User;
import com.superfercho.identity.domain.model.UserStatus;
import com.superfercho.identity.infrastructure.mail.ConsoleEmailSender;
import com.superfercho.identity.infrastructure.mail.InMemoryPasswordRecoveryAbuseGuard;
import com.superfercho.identity.infrastructure.security.Sha256SecureTokenGenerator;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PasswordRecoveryUseCasesTest {

    private static final Instant NOW = Instant.parse("2026-06-01T10:00:00Z");
    private static final String VALID_PASSWORD = "Luis123!";
    private static final String EMAIL = "customer@example.com";

    private InMemoryUserRepository users;
    private InMemoryPasswordRecoveryTokenRepository tokens;
    private RecordingEmailSender emails;
    private SecureTokenGenerator tokenGenerator;
    private StubPasswordHasher passwordHasher;
    private MutableClock clock;
    private PasswordRecoveryAbuseGuard abuseGuard;
    private RequestPasswordRecoveryUseCase request;
    private ResetPasswordUseCase reset;
    private UUID userId;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepository();
        tokens = new InMemoryPasswordRecoveryTokenRepository();
        emails = new RecordingEmailSender();
        tokenGenerator = new SequenceTokenGenerator();
        passwordHasher = new StubPasswordHasher();
        clock = new MutableClock(NOW);
        abuseGuard = new InMemoryPasswordRecoveryAbuseGuard(clock, 20, Duration.ofHours(1));
        userId = UUID.randomUUID();
        users.save(activeCustomer(userId, EMAIL));

        request = new RequestPasswordRecoveryUseCase(
                users,
                tokens,
                tokenGenerator,
                emails,
                abuseGuard,
                clock,
                Duration.ofMinutes(15),
                2,
                Duration.ofHours(24),
                "http://localhost:3000");
        reset = new ResetPasswordUseCase(tokens, users, tokenGenerator, passwordHasher, clock);
    }

    @Test
    void requestForExistingEmailSendsRecoveryLinkAndStoresHashOnly() {
        PasswordRecoveryRequestResult result =
                request.execute(new RequestPasswordRecoveryCommand(EMAIL, "127.0.0.1"));

        assertThat(result.message()).isEqualTo(PasswordRecoveryRequestResult.GENERIC_MESSAGE);
        assertThat(emails.messages).hasSize(1);
        assertThat(emails.messages.getFirst().to()).isEqualTo(EMAIL);
        assertThat(emails.messages.getFirst().textBody())
                .contains("http://localhost:3000/reset-password?token=token-1");
        assertThat(tokens.all()).hasSize(1);
        String storedHash = tokens.all().getFirst().tokenHash();
        assertThat(storedHash).isEqualTo(tokenGenerator.hash("token-1"));
        assertThat(storedHash).isNotEqualTo("token-1");
    }

    @Test
    void requestForUnknownEmailReturnsSameMessageWithoutSending() {
        PasswordRecoveryRequestResult result =
                request.execute(new RequestPasswordRecoveryCommand("missing@example.com", "127.0.0.1"));

        assertThat(result.message()).isEqualTo(PasswordRecoveryRequestResult.GENERIC_MESSAGE);
        assertThat(emails.messages).isEmpty();
        assertThat(tokens.all()).isEmpty();
    }

    @Test
    void requestForInactiveUserDoesNotRevealAccount() {
        users.save(inactiveCustomer(UUID.randomUUID(), "inactive@example.com"));

        PasswordRecoveryRequestResult result =
                request.execute(new RequestPasswordRecoveryCommand("inactive@example.com", "127.0.0.1"));

        assertThat(result.message()).isEqualTo(PasswordRecoveryRequestResult.GENERIC_MESSAGE);
        assertThat(emails.messages).isEmpty();
    }

    @Test
    void requestForDeletedUserDoesNotGenerateTokenOrEmail() {
        UUID deletedId = UUID.randomUUID();
        users.save(deletedCustomer(deletedId, "deleted@example.com"));

        PasswordRecoveryRequestResult result =
                request.execute(new RequestPasswordRecoveryCommand("deleted@example.com", "127.0.0.1"));

        assertThat(result.message()).isEqualTo(PasswordRecoveryRequestResult.GENERIC_MESSAGE);
        assertThat(emails.messages).isEmpty();
        assertThat(tokens.all()).isEmpty();
    }

    @Test
    void resetRejectsTokenWhenUserIsDeleted() {
        UUID deletedId = UUID.randomUUID();
        users.save(deletedCustomer(deletedId, "deleted-reset@example.com"));
        tokens.save(PasswordRecoveryToken.create(
                UUID.randomUUID(),
                deletedId,
                tokenGenerator.hash("leftover-token"),
                "127.0.0.1",
                NOW,
                NOW.plus(Duration.ofMinutes(15))));

        assertThatThrownBy(() -> reset.execute(new ResetPasswordCommand(
                        "leftover-token", VALID_PASSWORD, VALID_PASSWORD)))
                .isInstanceOf(InvalidPasswordRecoveryException.class);
        assertThat(users.findById(deletedId).orElseThrow().passwordHash()).isEqualTo("hash:old");
    }

    @Test
    void secondRequestInvalidatesPreviousToken() {
        request.execute(new RequestPasswordRecoveryCommand(EMAIL, "127.0.0.1"));
        request.execute(new RequestPasswordRecoveryCommand(EMAIL, "127.0.0.1"));

        assertThat(tokens.all()).hasSize(2);
        PasswordRecoveryToken first = tokens.findByTokenHash(tokenGenerator.hash("token-1")).orElseThrow();
        PasswordRecoveryToken second = tokens.findByTokenHash(tokenGenerator.hash("token-2")).orElseThrow();
        assertThat(first.isConsumed()).isTrue();
        assertThat(second.isUsable(NOW)).isTrue();

        assertThatThrownBy(() -> reset.execute(new ResetPasswordCommand("token-1", VALID_PASSWORD, VALID_PASSWORD)))
                .isInstanceOf(InvalidPasswordRecoveryException.class);
    }

    @Test
    void thirdRequestWithinWindowIsSilentlySkipped() {
        request.execute(new RequestPasswordRecoveryCommand(EMAIL, "127.0.0.1"));
        request.execute(new RequestPasswordRecoveryCommand(EMAIL, "127.0.0.1"));
        emails.messages.clear();

        PasswordRecoveryRequestResult result =
                request.execute(new RequestPasswordRecoveryCommand(EMAIL, "127.0.0.1"));

        assertThat(result.message()).isEqualTo(PasswordRecoveryRequestResult.GENERIC_MESSAGE);
        assertThat(emails.messages).isEmpty();
        assertThat(tokens.all()).hasSize(2);
    }

    @Test
    void expiredTokenCannotResetPassword() {
        request.execute(new RequestPasswordRecoveryCommand(EMAIL, "127.0.0.1"));
        clock.set(NOW.plus(Duration.ofMinutes(16)));

        assertThatThrownBy(() -> reset.execute(new ResetPasswordCommand("token-1", VALID_PASSWORD, VALID_PASSWORD)))
                .isInstanceOf(InvalidPasswordRecoveryException.class);
    }

    @Test
    void invalidTokenIsRejected() {
        assertThatThrownBy(
                        () -> reset.execute(new ResetPasswordCommand("unknown", VALID_PASSWORD, VALID_PASSWORD)))
                .isInstanceOf(InvalidPasswordRecoveryException.class);
    }

    @Test
    void successfulResetUpdatesPasswordAndConsumesToken() {
        request.execute(new RequestPasswordRecoveryCommand(EMAIL, "127.0.0.1"));

        reset.execute(new ResetPasswordCommand("token-1", VALID_PASSWORD, VALID_PASSWORD));

        assertThat(users.findById(userId).orElseThrow().passwordHash())
                .isEqualTo("hash:" + VALID_PASSWORD);
        assertThat(tokens.findByTokenHash(tokenGenerator.hash("token-1")).orElseThrow().isConsumed())
                .isTrue();
    }

    @Test
    void usedTokenCannotBeReused() {
        request.execute(new RequestPasswordRecoveryCommand(EMAIL, "127.0.0.1"));
        reset.execute(new ResetPasswordCommand("token-1", VALID_PASSWORD, VALID_PASSWORD));

        assertThatThrownBy(
                        () -> reset.execute(new ResetPasswordCommand("token-1", "Other123!", "Other123!")))
                .isInstanceOf(InvalidPasswordRecoveryException.class);
    }

    @Test
    void invalidNewPasswordIsRejected() {
        request.execute(new RequestPasswordRecoveryCommand(EMAIL, "127.0.0.1"));

        assertThatThrownBy(() -> reset.execute(new ResetPasswordCommand("token-1", "short", "short")))
                .isInstanceOf(InvalidPasswordRecoveryException.class);
    }

    @Test
    void mismatchedConfirmationIsRejected() {
        request.execute(new RequestPasswordRecoveryCommand(EMAIL, "127.0.0.1"));

        assertThatThrownBy(
                        () -> reset.execute(new ResetPasswordCommand("token-1", VALID_PASSWORD, "Luis123!!")))
                .isInstanceOf(InvalidPasswordRecoveryException.class);
    }

    @Test
    void blankEmailIsRejected() {
        assertThatThrownBy(() -> request.execute(new RequestPasswordRecoveryCommand(" ", "127.0.0.1")))
                .isInstanceOf(InvalidRegistrationException.class);
    }

    @Test
    void ipAbuseGuardBlocksExcessiveRequests() {
        PasswordRecoveryAbuseGuard strict =
                new InMemoryPasswordRecoveryAbuseGuard(clock, 1, Duration.ofHours(1));
        RequestPasswordRecoveryUseCase limited = new RequestPasswordRecoveryUseCase(
                users,
                tokens,
                tokenGenerator,
                emails,
                strict,
                clock,
                Duration.ofMinutes(15),
                2,
                Duration.ofHours(24),
                "http://localhost:3000");

        limited.execute(new RequestPasswordRecoveryCommand(EMAIL, "10.0.0.1"));
        assertThatThrownBy(() -> limited.execute(new RequestPasswordRecoveryCommand(EMAIL, "10.0.0.1")))
                .isInstanceOf(PasswordRecoveryRateLimitedException.class);
    }

    @Test
    void consoleEmailSenderFormatsRecoveryLog() {
        ConsoleEmailSender console = new ConsoleEmailSender();
        console.send(new EmailMessage(
                EMAIL,
                "Restablece tu contraseña de SuperFercho",
                "Usa este enlace:\nhttp://localhost:3000/reset-password?token=abc\n"));
        // No exception — adapter is side-effect logging only.
    }

    @Test
    void sha256GeneratorProducesUrlSafeOpaqueTokens() {
        Sha256SecureTokenGenerator generator = new Sha256SecureTokenGenerator();
        String raw = generator.generate();
        String hash = generator.hash(raw);

        assertThat(raw).isNotBlank();
        assertThat(hash).hasSize(64);
        assertThat(hash).isEqualTo(generator.hash(raw));
        assertThat(hash).isNotEqualTo(raw);
    }

    private static User activeCustomer(UUID id, String email) {
        return User.create(
                id,
                "CC",
                "100200300",
                "Customer",
                "User",
                email,
                "3001234567",
                "hash:old",
                Role.CUSTOMER,
                UserStatus.ACTIVE,
                NOW,
                NOW);
    }

    private static User inactiveCustomer(UUID id, String email) {
        return User.create(
                id,
                "CC",
                "100200301",
                "Inactive",
                "User",
                email,
                "3001234568",
                "hash:old",
                Role.CUSTOMER,
                UserStatus.INACTIVE,
                NOW,
                NOW);
    }

    private static User deletedCustomer(UUID id, String email) {
        return User.create(
                id,
                "CC",
                "100200302",
                "Deleted",
                "User",
                email,
                "3001234569",
                "hash:old",
                Role.CUSTOMER,
                UserStatus.INACTIVE,
                UUID.randomUUID(),
                NOW,
                NOW,
                NOW);
    }

    private static final class RecordingEmailSender implements EmailSender {
        private final List<EmailMessage> messages = new ArrayList<>();

        @Override
        public void send(EmailMessage message) {
            messages.add(message);
        }
    }

    private static final class StubPasswordHasher implements PasswordHasher {
        @Override
        public String hash(String rawPassword) {
            return "hash:" + rawPassword;
        }

        @Override
        public boolean matches(String rawPassword, String passwordHash) {
            return passwordHash.equals(hash(rawPassword));
        }
    }

    private static final class SequenceTokenGenerator implements SecureTokenGenerator {
        private final AtomicInteger sequence = new AtomicInteger();
        private final Sha256SecureTokenGenerator hasher = new Sha256SecureTokenGenerator();

        @Override
        public String generate() {
            return "token-" + sequence.incrementAndGet();
        }

        @Override
        public String hash(String rawToken) {
            return hasher.hash(rawToken);
        }
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void set(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
