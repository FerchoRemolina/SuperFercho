package com.superfercho.assistantops.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.superfercho.assistant.application.fercho.FerchoUsageLedgerUnavailableException;
import com.superfercho.assistant.application.fercho.FerchoLimits;
import com.superfercho.assistant.application.fercho.FerchoTurnDecision;
import com.superfercho.assistant.application.fercho.FerchoTurnDecisionType;
import com.superfercho.assistant.application.fercho.FerchoUsageGate;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(properties = "superfercho.scheduling.enabled=false")
@Testcontainers(disabledWithoutDocker = true)
class JpaFerchoUsageGateIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(
                            DockerImageName.parse("pgvector/pgvector:pg16")
                                    .asCompatibleSubstituteFor("postgres"))
                    .withDatabaseName("superfercho")
                    .withUsername("superfercho")
                    .withPassword("superfercho");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("superfercho.security.jwt.secret", () -> "test-only-superfercho-jwt-secret-key-32b");
        registry.add("superfercho.assistant.fercho.limits.max-requests-per-window", () -> "30");
        registry.add("superfercho.assistant.fercho.limits.window", () -> "24h");
        registry.add("superfercho.assistant.fercho.limits.pause", () -> "30m");
        registry.add("superfercho.assistant.fercho.limits.recidive-window", () -> "24h");
    }

    private static final Instant T0 = Instant.now();
    private static final FerchoLimits TEST_LIMITS =
            new FerchoLimits(3, Duration.ofHours(24), Duration.ofMinutes(30), Duration.ofHours(24));

    @Autowired
    private FerchoUsageGate gate;

    @Autowired
    private FerchoChatTurnJpaRepository turns;

    @Autowired
    private FerchoRestrictionJpaRepository restrictions;

    @Autowired
    private FerchoAdminAlertJpaRepository alerts;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID userId;

    @BeforeEach
    void cleanUp() {
        userId = UUID.randomUUID();
        jdbcTemplate.execute("delete from assistant.fercho_admin_actions");
        jdbcTemplate.execute("delete from assistant.fercho_admin_alerts");
        jdbcTemplate.execute("delete from assistant.fercho_restrictions");
        jdbcTemplate.execute("delete from assistant.fercho_chat_turns");
    }

    private long acceptedCount(UUID user) {
        return turns.countByUserIdAndStatusAndCreatedAtAfter(user, "ACCEPTED", T0);
    }

    private long rejectedCount(UUID user, String prefix) {
        return turns.countByUserIdAndStatusStartingWithAndCreatedAtBetween(user, prefix, T0, Instant.now());
    }

    private boolean hasActivePause(UUID user) {
        return restrictions.findFirstByUserIdAndLiftedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
                user, Instant.now()).isPresent();
    }

    @Test
    void acceptedTurnsBelowTheLimit() {
        for (int i = 0; i < 3; i++) {
            var decision = gate.evaluateAndReserve(userId, TEST_LIMITS, Instant.now());
            assertThat(decision.accepted()).isTrue();
            assertNotNull(decision.turnId());
        }
        assertThat(acceptedCount(userId)).isEqualTo(3);
    }

    @Test
    void pauseActivatesAtTheThresholdAndRejectsWithoutAccepting() {
        for (int i = 0; i < 3; i++) {
            gate.evaluateAndReserve(userId, TEST_LIMITS, Instant.now());
        }
        var decision = gate.evaluateAndReserve(userId, TEST_LIMITS, Instant.now());
        assertThat(decision.accepted()).isFalse();
        assertThat(decision.type()).isEqualTo(FerchoTurnDecisionType.PAUSED);
        assertThat(decision.pausedUntil()).isNotNull();
        assertThat(acceptedCount(userId)).isEqualTo(3);
        assertThat(hasActivePause(userId)).isTrue();
    }

    @Test
    void rejectsDuringPauseWithoutAccepting() {
        // Primer límite → pausa
        for (int i = 0; i < 3; i++) {
            gate.evaluateAndReserve(userId, TEST_LIMITS, Instant.now());
        }
        gate.evaluateAndReserve(userId, TEST_LIMITS, Instant.now()); // crea pausa
        // Durante la pausa: rechaza sin aceptar
        var rejected = gate.evaluateAndReserve(userId, TEST_LIMITS, Instant.now());
        assertThat(rejected.accepted()).isFalse();
        assertThat(rejected.type()).isEqualTo(FerchoTurnDecisionType.PAUSED);
        assertThat(acceptedCount(userId)).isEqualTo(3);
    }

    @Test
    void windowExpiresAndClientResumes() {
        // Insertar 3 turnos aceptados hace 25 horas (fuera de la ventana de 24 h).
        var old = Instant.now().minus(Duration.ofHours(25));
        for (int i = 0; i < 3; i++) {
            jdbcTemplate.update(
                    "insert into assistant.fercho_chat_turns (id, user_id, status, llm_calls, created_at) "
                            + "values (?, ?, 'ACCEPTED', 0, ?)",
                    UUID.randomUUID(), userId, java.sql.Timestamp.from(old));
        }
        // Ventana móvil no cuenta los turnos viejos → acepta.
        var decision = gate.evaluateAndReserve(userId, TEST_LIMITS, Instant.now());
        assertThat(decision.accepted()).isTrue();
        assertThat(acceptedCount(userId)).isEqualTo(1);
    }

    @Test
    void pauseDoesNotResetTheWindow() {
        for (int i = 0; i < 3; i++) {
            gate.evaluateAndReserve(userId, TEST_LIMITS, Instant.now());
        }
        // Pausa de 30 min: sin avanzar el reloj, el 4.º turno rechaza.
        var decision = gate.evaluateAndReserve(userId, TEST_LIMITS, Instant.now());
        assertThat(decision.accepted()).isFalse();
        assertThat(acceptedCount(userId)).isEqualTo(3);
    }

    @Test
    void clientIsolationBetweenUsers() {
        var userB = UUID.randomUUID();
        for (int i = 0; i < 3; i++) {
            gate.evaluateAndReserve(userId, TEST_LIMITS, Instant.now());
        }
        var decisionB = gate.evaluateAndReserve(userB, TEST_LIMITS, Instant.now());
        assertThat(decisionB.accepted()).isTrue();
        assertThat(acceptedCount(userB)).isEqualTo(1);
        assertThat(acceptedCount(userId)).isEqualTo(3);
    }

    @Test
    void firstAutoPauseDoesNotCreateARecidiveAlert() {
        for (int i = 0; i < 3; i++) {
            gate.evaluateAndReserve(userId, TEST_LIMITS, Instant.now());
        }
        var decision = gate.evaluateAndReserve(userId, TEST_LIMITS, Instant.now());
        assertThat(decision.accepted()).isFalse();
        assertThat(hasActivePause(userId)).isTrue();
        assertThat(alerts.countByUserIdAndStatus(userId, "OPEN")).isZero();
    }

    @Test
    void recidiveCreatesAlertOnce() {
        var userC = UUID.randomUUID();
        for (int i = 0; i < 3; i++) {
            gate.evaluateAndReserve(userC, TEST_LIMITS, Instant.now());
        }
        gate.evaluateAndReserve(userC, TEST_LIMITS, Instant.now());
        assertThat(alerts.countByUserIdAndStatus(userC, "OPEN")).isZero();

        jdbcTemplate.update(
                "update assistant.fercho_restrictions set expires_at = ? where user_id = ? and lifted_at is null",
                java.sql.Timestamp.from(Instant.now().minusSeconds(1)),
                userC);

        var secondLimit = gate.evaluateAndReserve(userC, TEST_LIMITS, Instant.now());
        assertThat(secondLimit.accepted()).isFalse();
        assertThat(alerts.countByUserIdAndStatus(userC, "OPEN")).isEqualTo(1);

        gate.evaluateAndReserve(userC, TEST_LIMITS, Instant.now());
        assertThat(alerts.countByUserIdAndStatus(userC, "OPEN")).isEqualTo(1);
    }

    @Test
    void concurrentRequestsDoNotExceedTheLimit() throws Exception {
        var limit = TEST_LIMITS.maxRequestsPerWindow();
        var threads = limit + 5;
        var latch = new java.util.concurrent.CountDownLatch(1);
        var executor = java.util.concurrent.Executors.newFixedThreadPool(threads);
        var acceptedAtomic = new java.util.concurrent.atomic.AtomicInteger(0);
        var futures = java.util.stream.IntStream.range(0, threads).mapToObj(i -> executor.submit(() -> {
            latch.await();
            var decision = gate.evaluateAndReserve(userId, TEST_LIMITS, Instant.now());
            if (decision.accepted()) {
                acceptedAtomic.incrementAndGet();
            }
            return decision;
        })).toList();
        latch.countDown();
        for (var future : futures) {
            future.get(15, java.util.concurrent.TimeUnit.SECONDS);
        }
        executor.shutdown();
        executor.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS);

        var dbAccepted = acceptedCount(userId);
        assertThat(dbAccepted).isEqualTo(limit);
        assertThat(acceptedAtomic.get()).isEqualTo(limit);
    }

    @Test
    void concurrentRequestsFromDifferentClientsDoNotShareCounters() throws Exception {
        var userA = UUID.randomUUID();
        var userB = UUID.randomUUID();
        var executor = Executors.newFixedThreadPool(2);
        var latch = new CountDownLatch(1);
        var futures = List.of(userA, userB).stream().map(user -> executor.submit(() -> {
            latch.await();
            return gate.evaluateAndReserve(user, TEST_LIMITS, Instant.now());
        })).toList();
        latch.countDown();
        for (var future : futures) {
            future.get(10, TimeUnit.SECONDS);
        }
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        var countA = acceptedCount(userA);
        var countB = acceptedCount(userB);
        assertThat(countA).isEqualTo(1);
        assertThat(countB).isEqualTo(1);
    }

    private static void assertNotNull(Object value) {
        org.assertj.core.api.Assertions.assertThat(value).isNotNull();
    }
}
