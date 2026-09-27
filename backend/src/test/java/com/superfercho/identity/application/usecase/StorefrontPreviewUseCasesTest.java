package com.superfercho.identity.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.superfercho.identity.application.dto.StorefrontPreviewSessionResult;
import com.superfercho.identity.application.exception.StorefrontPreviewForbiddenException;
import com.superfercho.identity.application.exception.StorefrontPreviewNotFoundException;
import com.superfercho.identity.application.fakes.InMemoryAddressRepository;
import com.superfercho.identity.application.fakes.InMemoryCustomerPreviewRepository;
import com.superfercho.identity.application.fakes.InMemoryUserRepository;
import com.superfercho.identity.application.port.AccessTokenIssuer;
import com.superfercho.identity.application.port.CurrentUserProvider;
import com.superfercho.identity.application.port.IssuedAccessToken;
import com.superfercho.identity.application.port.PasswordHasher;
import com.superfercho.identity.application.port.PreviewAssistantCleanupPort;
import com.superfercho.identity.application.port.PreviewOrdersCleanupPort;
import com.superfercho.identity.application.port.PreviewShoppingCleanupPort;
import com.superfercho.identity.domain.model.CustomerPreviewStatus;
import com.superfercho.identity.domain.model.Role;
import com.superfercho.identity.domain.model.User;
import com.superfercho.identity.domain.model.UserStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StorefrontPreviewUseCasesTest {

    private static final Instant NOW = Instant.parse("2026-06-01T10:00:00Z");

    private InMemoryUserRepository users;
    private InMemoryCustomerPreviewRepository previews;
    private InMemoryAddressRepository addresses;
    private RecordingCleanup ordersCleanup;
    private RecordingCleanup shoppingCleanup;
    private RecordingCleanup assistantCleanup;
    private MutableClock clock;
    private UUID adminId;
    private StartStorefrontPreviewUseCase start;
    private GetStorefrontPreviewUseCase get;
    private ExitStorefrontPreviewUseCase exit;
    private FinalizeStorefrontPreviewUseCase finalize;
    private ExpireStorefrontPreviewsUseCase expire;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepository();
        previews = new InMemoryCustomerPreviewRepository();
        addresses = new InMemoryAddressRepository();
        ordersCleanup = new RecordingCleanup();
        shoppingCleanup = new RecordingCleanup();
        assistantCleanup = new RecordingCleanup();
        clock = new MutableClock(NOW);
        adminId = UUID.randomUUID();
        users.save(adminUser(adminId, "admin@example.com"));

        finalize = new FinalizeStorefrontPreviewUseCase(
                previews,
                ordersCleanup,
                shoppingCleanup,
                assistantCleanup,
                addresses,
                users,
                clock);
        CurrentUserProvider currentUser = () -> adminId;
        AccessTokenIssuer tokens = new StubAccessTokenIssuer(clock);
        PasswordHasher hasher = new StubPasswordHasher();
        start = new StartStorefrontPreviewUseCase(
                currentUser, users, previews, hasher, tokens, finalize, clock);
        get = new GetStorefrontPreviewUseCase(currentUser, users, previews, finalize, clock);
        exit = new ExitStorefrontPreviewUseCase(currentUser, users, previews, finalize);
        expire = new ExpireStorefrontPreviewsUseCase(previews, finalize, clock);
    }

    @Test
    void startCreatesTemporaryCustomerAndTwentyMinuteExpiry() {
        StorefrontPreviewSessionResult session = start.execute();

        assertThat(session.adminUserId()).isEqualTo(adminId);
        assertThat(session.temporaryCustomerId()).isNotEqualTo(adminId);
        assertThat(session.temporaryCustomerRole()).isEqualTo(Role.CUSTOMER);
        assertThat(session.status()).isEqualTo(CustomerPreviewStatus.ACTIVE);
        assertThat(session.previewExpiresAt()).isEqualTo(NOW.plusSeconds(20 * 60));
        assertThat(session.remainingSeconds()).isEqualTo(20 * 60);
        assertThat(session.accessToken()).contains(session.temporaryCustomerId().toString());
        assertThat(session.adminAccessToken()).contains(adminId.toString());
        assertThat(session.adminAccessToken()).contains("ADMIN");
        assertThat(session.accessTokenExpiresAt()).isEqualTo(NOW.plusSeconds(20 * 60));
        assertThat(session.adminAccessTokenExpiresAt()).isEqualTo(NOW.plusSeconds(20 * 60));
        assertThat(users.findById(adminId).orElseThrow().role()).isEqualTo(Role.ADMIN);
        assertThat(users.findById(session.temporaryCustomerId()).orElseThrow().role())
                .isEqualTo(Role.CUSTOMER);
    }

    @Test
    void secondStartReusesSameActivePreviewAndCustomer() {
        StorefrontPreviewSessionResult first = start.execute();
        StorefrontPreviewSessionResult second = start.execute();

        assertThat(second.previewId()).isEqualTo(first.previewId());
        assertThat(second.temporaryCustomerId()).isEqualTo(first.temporaryCustomerId());
        assertThat(second.adminAccessToken()).isNotEqualTo(first.adminAccessToken());
        assertThat(previews.findActiveByAdminUserId(adminId)).isPresent();
    }

    @Test
    void startIssuesFreshAdminTokenAlignedWithCustomerTokenLifetime() {
        clock.set(NOW.plusSeconds(12 * 60));
        StorefrontPreviewSessionResult session = start.execute();

        assertThat(session.adminAccessTokenExpiresAt()).isEqualTo(NOW.plusSeconds(12 * 60 + 20 * 60));
        assertThat(session.accessTokenExpiresAt()).isEqualTo(NOW.plusSeconds(12 * 60 + 20 * 60));
        assertThat(session.previewExpiresAt()).isEqualTo(NOW.plusSeconds(12 * 60 + 20 * 60));
    }

    @Test
    void customerCannotStartPreview() {
        UUID customerId = UUID.randomUUID();
        users.save(customerUser(customerId, "customer@example.com"));
        StartStorefrontPreviewUseCase customerStart = new StartStorefrontPreviewUseCase(
                () -> customerId,
                users,
                previews,
                new StubPasswordHasher(),
                new StubAccessTokenIssuer(clock),
                finalize,
                clock);

        assertThatThrownBy(customerStart::execute)
                .isInstanceOf(StorefrontPreviewForbiddenException.class);
    }

    @Test
    void exitClosesPreviewDeletesTemporaryUserAndPreviewRow() {
        StorefrontPreviewSessionResult session = start.execute();
        UUID temporaryCustomerId = session.temporaryCustomerId();
        UUID previewId = session.previewId();

        exit.execute();

        assertThat(previews.findById(previewId)).isEmpty();
        assertThat(users.findById(temporaryCustomerId)).isEmpty();
        assertThat(users.findById(adminId)).isPresent();
        assertThat(users.findById(adminId).orElseThrow().status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(ordersCleanup.calls).containsExactly(temporaryCustomerId);
        assertThat(shoppingCleanup.calls).containsExactly(temporaryCustomerId);
        assertThat(assistantCleanup.calls).containsExactly(temporaryCustomerId);
        assertThatThrownBy(exit::execute).isInstanceOf(StorefrontPreviewNotFoundException.class);
        assertThat(ordersCleanup.calls).hasSize(1);
    }

    @Test
    void getExpiresAndCleansWhenPastExpiresAt() {
        StorefrontPreviewSessionResult session = start.execute();
        UUID temporaryCustomerId = session.temporaryCustomerId();
        UUID previewId = session.previewId();
        clock.set(NOW.plusSeconds(20 * 60));

        assertThatThrownBy(get::execute).isInstanceOf(StorefrontPreviewNotFoundException.class);
        assertThat(previews.findById(previewId)).isEmpty();
        assertThat(users.findById(temporaryCustomerId)).isEmpty();
        assertThat(users.findById(adminId)).isPresent();
        assertThat(ordersCleanup.calls).containsExactly(temporaryCustomerId);
    }

    @Test
    void sweeperExpiresAbandonedPreviewsIdempotently() {
        StorefrontPreviewSessionResult session = start.execute();
        UUID temporaryCustomerId = session.temporaryCustomerId();
        UUID previewId = session.previewId();
        clock.set(NOW.plusSeconds(20 * 60 + 1));

        assertThat(expire.execute()).isEqualTo(1);
        assertThat(expire.execute()).isEqualTo(0);
        assertThat(previews.findById(previewId)).isEmpty();
        assertThat(users.findById(temporaryCustomerId)).isEmpty();
        assertThat(users.findById(adminId)).isPresent();
        assertThat(ordersCleanup.calls).containsExactly(temporaryCustomerId);
    }

    @Test
    void onlyClaimingWriterRunsCleanup() {
        StorefrontPreviewSessionResult session = start.execute();
        UUID temporaryCustomerId = session.temporaryCustomerId();

        assertThat(finalize.execute(session.previewId())).isTrue();
        assertThat(finalize.execute(session.previewId())).isFalse();
        assertThat(ordersCleanup.calls).containsExactly(temporaryCustomerId);
        assertThat(previews.findById(session.previewId())).isEmpty();
        assertThat(users.findById(temporaryCustomerId)).isEmpty();
    }

    @Test
    void cleanupOfOnePreviewDoesNotAffectAnotherPreviewOrNormalCustomer() {
        StorefrontPreviewSessionResult previewA = start.execute();
        UUID otherAdminId = UUID.randomUUID();
        users.save(adminUser(otherAdminId, "admin-b@example.com"));
        StartStorefrontPreviewUseCase startB = new StartStorefrontPreviewUseCase(
                () -> otherAdminId,
                users,
                previews,
                new StubPasswordHasher(),
                new StubAccessTokenIssuer(clock),
                finalize,
                clock);
        StorefrontPreviewSessionResult previewB = startB.execute();
        UUID normalCustomerId = UUID.randomUUID();
        users.save(customerUser(normalCustomerId, "normal@example.com"));

        exit.execute();

        assertThat(previews.findById(previewA.previewId())).isEmpty();
        assertThat(users.findById(previewA.temporaryCustomerId())).isEmpty();
        assertThat(previews.findById(previewB.previewId())).isPresent();
        assertThat(users.findById(previewB.temporaryCustomerId())).isPresent();
        assertThat(users.findById(normalCustomerId)).isPresent();
        assertThat(users.findById(adminId)).isPresent();
        assertThat(users.findById(otherAdminId)).isPresent();
    }

    @Test
    void activityDoesNotExtendDuration() {
        StorefrontPreviewSessionResult first = start.execute();
        clock.set(NOW.plusSeconds(5 * 60));
        StorefrontPreviewSessionResult reused = start.execute();

        assertThat(reused.previewExpiresAt()).isEqualTo(first.previewExpiresAt());
        assertThat(reused.remainingSeconds()).isEqualTo(15 * 60);
    }

    private static User adminUser(UUID id, String email) {
        return User.create(
                id,
                "CC",
                "ADMIN" + id.toString().substring(0, 8),
                "Admin",
                "User",
                email,
                "3000000000",
                "hash",
                Role.ADMIN,
                UserStatus.ACTIVE,
                NOW,
                NOW);
    }

    private static User customerUser(UUID id, String email) {
        return User.create(
                id,
                "CC",
                "CUST" + id.toString().substring(0, 8),
                "Customer",
                "User",
                email,
                "3000000001",
                "hash",
                Role.CUSTOMER,
                UserStatus.ACTIVE,
                NOW,
                NOW);
    }

    private static final class RecordingCleanup
            implements PreviewOrdersCleanupPort, PreviewShoppingCleanupPort, PreviewAssistantCleanupPort {

        private final List<UUID> calls = new ArrayList<>();
        private final AtomicInteger count = new AtomicInteger();

        @Override
        public void cancelAndDeleteAllForCustomer(UUID customerId) {
            calls.add(customerId);
            count.incrementAndGet();
        }

        @Override
        public void deleteAllForCustomer(UUID customerId) {
            calls.add(customerId);
            count.incrementAndGet();
        }

        @Override
        public void deleteAllForUser(UUID userId) {
            calls.add(userId);
            count.incrementAndGet();
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

    private static final class StubAccessTokenIssuer implements AccessTokenIssuer {

        private final MutableClock clock;
        private final AtomicInteger sequence = new AtomicInteger();

        private StubAccessTokenIssuer(MutableClock clock) {
            this.clock = clock;
        }

        @Override
        public IssuedAccessToken issue(UUID userId, Role role) {
            return issue(userId, role, null);
        }

        @Override
        public IssuedAccessToken issue(UUID userId, Role role, UUID previewId) {
            String token = userId + ":" + role + ":" + previewId + ":" + sequence.incrementAndGet();
            return new IssuedAccessToken(token, clock.instant().plusSeconds(20 * 60));
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
