package com.superfercho.identity.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.superfercho.identity.application.dto.PasswordRecoveryRequestResult;
import com.superfercho.identity.application.dto.RequestPasswordRecoveryByDocumentCommand;
import com.superfercho.identity.application.exception.InvalidRegistrationException;
import com.superfercho.identity.application.fakes.InMemoryCustomerRecordRepository;
import com.superfercho.identity.application.fakes.InMemoryPasswordRecoveryTokenRepository;
import com.superfercho.identity.application.fakes.InMemoryUserRepository;
import com.superfercho.identity.application.port.EmailMessage;
import com.superfercho.identity.application.port.EmailSender;
import com.superfercho.identity.application.port.PasswordRecoveryAbuseGuard;
import com.superfercho.identity.application.port.SecureTokenGenerator;
import com.superfercho.identity.application.service.PasswordRecoveryIssuer;
import com.superfercho.identity.domain.model.CustomerRecord;
import com.superfercho.identity.domain.model.Role;
import com.superfercho.identity.domain.model.User;
import com.superfercho.identity.domain.model.UserStatus;
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

class RequestPasswordRecoveryByDocumentUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-06-01T10:00:00Z");
    private static final String DOCUMENT_TYPE = "CC";
    private static final String DOCUMENT_NUMBER = "12345678";
    private static final String EMAIL = "live@example.com";

    private InMemoryUserRepository users;
    private InMemoryCustomerRecordRepository customerRecords;
    private InMemoryPasswordRecoveryTokenRepository tokens;
    private RecordingEmailSender emails;
    private RequestPasswordRecoveryByDocumentUseCase useCase;
    private UUID recordId;
    private UUID liveUserId;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepository();
        customerRecords = new InMemoryCustomerRecordRepository();
        tokens = new InMemoryPasswordRecoveryTokenRepository();
        emails = new RecordingEmailSender();
        SecureTokenGenerator tokenGenerator = new SequenceTokenGenerator();
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        PasswordRecoveryAbuseGuard abuseGuard =
                new InMemoryPasswordRecoveryAbuseGuard(clock, 20, Duration.ofHours(1));
        PasswordRecoveryIssuer issuer = new PasswordRecoveryIssuer(
                tokens,
                tokenGenerator,
                emails,
                clock,
                Duration.ofMinutes(15),
                2,
                Duration.ofHours(24),
                "http://localhost:3000");
        useCase = new RequestPasswordRecoveryByDocumentUseCase(
                customerRecords, users, issuer, abuseGuard);

        recordId = UUID.randomUUID();
        liveUserId = UUID.randomUUID();
        customerRecords.save(CustomerRecord.create(
                recordId, DOCUMENT_TYPE, DOCUMENT_NUMBER, "Ada", "Lovelace", NOW, NOW));
        users.save(customer(liveUserId, EMAIL, Role.CUSTOMER, UserStatus.ACTIVE, recordId, null));
    }

    @Test
    void liveCustomerByDocumentIssuesRecoveryWithoutExposingEmail() {
        PasswordRecoveryRequestResult result = useCase.execute(command(DOCUMENT_TYPE, DOCUMENT_NUMBER));

        assertThat(result.message()).isEqualTo(PasswordRecoveryRequestResult.GENERIC_MESSAGE_BY_DOCUMENT);
        assertThat(result.message()).doesNotContain(EMAIL);
        assertThat(emails.messages).hasSize(1);
        assertThat(emails.messages.getFirst().to()).isEqualTo(EMAIL);
        assertThat(tokens.all()).hasSize(1);
        assertThat(tokens.all().getFirst().userId()).isEqualTo(liveUserId);
    }

    @Test
    void unknownDocumentReturnsGenericWithoutTokenOrEmail() {
        PasswordRecoveryRequestResult result = useCase.execute(command(DOCUMENT_TYPE, "99999999"));

        assertThat(result.message()).isEqualTo(PasswordRecoveryRequestResult.GENERIC_MESSAGE_BY_DOCUMENT);
        assertThat(emails.messages).isEmpty();
        assertThat(tokens.all()).isEmpty();
    }

    @Test
    void onlyDeletedUsersForDocumentReturnsGenericWithoutTokenOrEmail() {
        users.save(customer(
                liveUserId,
                EMAIL,
                Role.CUSTOMER,
                UserStatus.INACTIVE,
                recordId,
                NOW));

        PasswordRecoveryRequestResult result = useCase.execute(command(DOCUMENT_TYPE, DOCUMENT_NUMBER));

        assertThat(result.message()).isEqualTo(PasswordRecoveryRequestResult.GENERIC_MESSAGE_BY_DOCUMENT);
        assertThat(emails.messages).isEmpty();
        assertThat(tokens.all()).isEmpty();
    }

    @Test
    void inactiveLiveUserWithoutDeletedAtDoesNotIssueRecovery() {
        users.save(customer(
                liveUserId,
                EMAIL,
                Role.CUSTOMER,
                UserStatus.INACTIVE,
                recordId,
                null));

        PasswordRecoveryRequestResult result = useCase.execute(command(DOCUMENT_TYPE, DOCUMENT_NUMBER));

        assertThat(result.message()).isEqualTo(PasswordRecoveryRequestResult.GENERIC_MESSAGE_BY_DOCUMENT);
        assertThat(emails.messages).isEmpty();
        assertThat(tokens.all()).isEmpty();
    }

    @Test
    void adminIsNotACandidateEvenIfSomehowLinked() {
        users.save(customer(
                liveUserId,
                "admin@example.com",
                Role.ADMIN,
                UserStatus.ACTIVE,
                recordId,
                null));

        PasswordRecoveryRequestResult result = useCase.execute(command(DOCUMENT_TYPE, DOCUMENT_NUMBER));

        assertThat(result.message()).isEqualTo(PasswordRecoveryRequestResult.GENERIC_MESSAGE_BY_DOCUMENT);
        assertThat(emails.messages).isEmpty();
        assertThat(tokens.all()).isEmpty();
    }

    @Test
    void previewStyleCustomerWithoutCustomerRecordIsNotReachableByDocument() {
        UUID previewId = UUID.randomUUID();
        users.save(User.create(
                previewId,
                DOCUMENT_TYPE,
                "87654321",
                "Preview",
                "Temp",
                "preview@temp.superfercho.local",
                "3001111111",
                "hash:old",
                Role.CUSTOMER,
                UserStatus.ACTIVE,
                NOW,
                NOW));

        PasswordRecoveryRequestResult result = useCase.execute(command(DOCUMENT_TYPE, "87654321"));

        assertThat(result.message()).isEqualTo(PasswordRecoveryRequestResult.GENERIC_MESSAGE_BY_DOCUMENT);
        assertThat(emails.messages).isEmpty();
        assertThat(tokens.all()).isEmpty();
    }

    @Test
    void invalidDocumentIsRejectedWithoutSideEffects() {
        assertThatThrownBy(() -> useCase.execute(command("XX", DOCUMENT_NUMBER)))
                .isInstanceOf(InvalidRegistrationException.class);
        assertThat(emails.messages).isEmpty();
        assertThat(tokens.all()).isEmpty();
    }

    private static RequestPasswordRecoveryByDocumentCommand command(
            String documentType, String documentNumber) {
        return new RequestPasswordRecoveryByDocumentCommand(documentType, documentNumber, "127.0.0.1");
    }

    private static User customer(
            UUID id,
            String email,
            Role role,
            UserStatus status,
            UUID customerRecordId,
            Instant deletedAt) {
        return User.create(
                id,
                DOCUMENT_TYPE,
                DOCUMENT_NUMBER,
                "Ada",
                "Lovelace",
                email,
                "3001234567",
                "hash:old",
                role,
                status,
                customerRecordId,
                deletedAt,
                NOW,
                deletedAt == null ? NOW : deletedAt);
    }

    private static final class RecordingEmailSender implements EmailSender {
        private final List<EmailMessage> messages = new ArrayList<>();

        @Override
        public void send(EmailMessage message) {
            messages.add(message);
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
}
