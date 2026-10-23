package com.superfercho.assistantops.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.superfercho.assistant.application.fercho.FerchoAlertDecision;
import com.superfercho.assistant.application.fercho.FerchoAlertRecord;
import com.superfercho.assistant.application.fercho.FerchoRestrictionRecord;
import com.superfercho.assistant.application.fercho.FerchoSupervisionPort;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(properties = "superfercho.scheduling.enabled=false")
@Testcontainers(disabledWithoutDocker = true)
class FerchoSupervisionAdapterTest {

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

    private static final Instant NOW = Instant.now();
    private static final UUID USER_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_B = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ADMIN = UUID.fromString("99999999-9999-9999-9999-999999999999");

    @Autowired
    private FerchoSupervisionPort supervision;

    @Autowired
    private FerchoRestrictionJpaRepository restrictions;

    @Autowired
    private FerchoAdminAlertJpaRepository alerts;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.execute("delete from assistant.fercho_admin_actions");
        jdbcTemplate.execute("delete from assistant.fercho_admin_alerts");
        jdbcTemplate.execute("delete from assistant.fercho_restrictions");
        jdbcTemplate.execute("delete from assistant.fercho_chat_turns");
    }

    private UUID saveOpenAlert(UUID userId) {
        var id = UUID.randomUUID();
        alerts.save(new FerchoAdminAlertEntity(
                id, userId, "OPEN", "RECIDIVE_LIMIT_EXCEEDED",
                NOW.minus(Duration.ofHours(1)), 3, 4, NOW, null));
        return id;
    }

    private UUID saveAutoPause(UUID userId, Instant createdAt, Instant expiresAt) {
        var id = UUID.randomUUID();
        restrictions.save(new FerchoRestrictionEntity(
                id, userId, "AUTO_PAUSE", "LIMIT_EXCEEDED", null, expiresAt, null, createdAt));
        return id;
    }

    @Test
    void persistsAndFindsAlertsFilteredByStatus() {
        var openId = saveOpenAlert(USER_A);
        var found = supervision.findAlerts("OPEN", 50);
        assertThat(found).extracting(FerchoAlertRecord::id).contains(openId);
        var all = supervision.findAlerts(null, 50);
        assertThat(all).extracting(FerchoAlertRecord::id).contains(openId);
        var detail = supervision.findAlert(openId).orElseThrow();
        assertThat(detail.userId()).isEqualTo(USER_A);
        assertThat(detail.reason()).isEqualTo("RECIDIVE_LIMIT_EXCEEDED");
        assertThat(detail.acceptedRequests()).isEqualTo(3);
    }

    @Test
    void enforcesOneOpenAlertPerClientAtTheDatabaseLevel() {
        var alertId = saveOpenAlert(USER_A);
        var duplicate = new FerchoAdminAlertEntity(
                UUID.randomUUID(), USER_A, "OPEN", "RECIDIVE_LIMIT_EXCEEDED",
                NOW.minus(Duration.ofHours(1)), 3, 4, NOW, null);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> alerts.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
        supervision.findAlert(alertId).orElseThrow();
    }

    @Test
    void activeRestrictionsExcludeExpiredOnesButHistoryIsPreserved() {
        var expiredId = saveAutoPause(USER_A, NOW.minus(Duration.ofHours(25)), NOW.minus(Duration.ofHours(1)));
        var active = supervision.findActiveRestrictions(50);
        assertThat(active).extracting(FerchoRestrictionRecord::id).doesNotContain(expiredId);
        var history = supervision.findRestrictionsForUser(USER_A, 12);
        assertThat(history).extracting(FerchoRestrictionRecord::id).contains(expiredId);
    }

    @Test
    void isolatesRestrictionsBetweenClients() {
        saveAutoPause(USER_A, NOW, NOW.plus(Duration.ofHours(2)));
        var forB = supervision.findRestrictionsForUser(USER_B, 12);
        var activeForB = supervision.findActiveRestrictions(50);
        assertThat(forB).isEmpty();
        assertThat(activeForB).extracting(FerchoRestrictionRecord::userId).doesNotContain(USER_B);
    }

    @Test
    void dismissingAnAlertClosesItWithAudit() {
        var alertId = saveOpenAlert(USER_A);
        supervision.resolveAlert(alertId, FerchoAlertDecision.DISMISS, ADMIN, "falso positivo", null, NOW);
        var detail = supervision.findAlert(alertId).orElseThrow();
        assertThat(detail.status()).isEqualTo("DISMISSED");
        assertThat(detail.decidedBy()).isEqualTo(ADMIN);
        assertThat(detail.decisionNote()).isEqualTo("falso positivo");
    }

    @Test
    void isIdempotentWhenResolvingAnAlreadyDecidedAlert() {
        var alertId = saveOpenAlert(USER_A);
        supervision.resolveAlert(alertId, FerchoAlertDecision.DISMISS, ADMIN, "primera", null, NOW);
        var second = supervision.resolveAlert(alertId, FerchoAlertDecision.WARN, ADMIN, "segunda", null, NOW);
        assertThat(second).isNull();
        var detail = supervision.findAlert(alertId).orElseThrow();
        assertThat(detail.status()).isEqualTo("DISMISSED");
    }

    @Test
    void suspensionDecisionCreatesARestrictionThatExpiresAutomatically() {
        var alertId = saveOpenAlert(USER_B);
        var restrictionId = supervision.resolveAlert(
                alertId, FerchoAlertDecision.SUSPEND, ADMIN, "abuso", Duration.ofHours(2), NOW);
        assertNotNull(restrictionId);
        var restriction = supervision.findRestriction(restrictionId).orElseThrow();
        assertThat(restriction.type()).isEqualTo("ADMIN_SUSPENSION");
        assertThat(restriction.userId()).isEqualTo(USER_B);
        assertThat(restriction.expiresAt()).isAfter(NOW.plus(Duration.ofHours(2)).minusSeconds(1));
        assertThat(restriction.expiresAt()).isBefore(NOW.plus(Duration.ofHours(2)).plusSeconds(1));
        // Vigente inmediatamente y vencida después de 2 h sin intervención.
        var active = supervision.findActiveRestrictions(50);
        assertThat(active).extracting(FerchoRestrictionRecord::id).contains(restrictionId);
        org.assertj.core.api.Assertions.assertThat(
                restrictions.findById(restrictionId).orElseThrow().activeAt(NOW.plus(Duration.ofHours(3))))
                .isFalse();
        var detail = supervision.findAlert(alertId).orElseThrow();
        assertThat(detail.status()).isEqualTo("SUSPENDED");
        assertThat(detail.restrictionId()).isEqualTo(restrictionId);
    }

    @Test
    void liftingASuspensionPreservesHistoryAndAuditsTheLift() {
        var alertId = saveOpenAlert(USER_B);
        var restrictionId = supervision.resolveAlert(
                alertId, FerchoAlertDecision.SUSPEND, ADMIN, "abuso", Duration.ofHours(2), NOW);
        supervision.liftSuspension(restrictionId, ADMIN, "revisión manual", NOW.plus(Duration.ofMinutes(5)));
        var restriction = supervision.findRestriction(restrictionId).orElseThrow();
        assertThat(restriction.liftedAt()).isNotNull();
        assertThat(restriction.liftedBy()).isEqualTo(ADMIN);
        assertThat(restriction.note()).isEqualTo("abuso");
        var active = supervision.findActiveRestrictions(50);
        assertThat(active).extracting(FerchoRestrictionRecord::id).doesNotContain(restrictionId);
        var history = supervision.findRestrictionsForUser(USER_B, 12);
        assertThat(history).extracting(FerchoRestrictionRecord::id).contains(restrictionId);
    }

    @Test
    void liftsAreIdempotentForAlreadyLiftedRestrictions() {
        var alertId = saveOpenAlert(USER_B);
        var restrictionId = supervision.resolveAlert(
                alertId, FerchoAlertDecision.SUSPEND, ADMIN, "abuso", Duration.ofHours(2), NOW);
        supervision.liftSuspension(restrictionId, ADMIN, "primera", NOW);
        org.assertj.core.api.Assertions.assertThatCode(() ->
                supervision.liftSuspension(restrictionId, ADMIN, "segunda", NOW))
                .doesNotThrowAnyException();
    }

    @Test
    void clientUsageSummarizesWindowAndExposesTheActiveRestriction() {
        var restrictionId = saveAutoPause(USER_A, NOW.minus(Duration.ofMinutes(1)), NOW.plus(Duration.ofMinutes(30)));
        var summary = supervision.clientUsage(
                USER_A, NOW.minus(Duration.ofHours(24)), NOW);
        assertThat(summary.userId()).isEqualTo(USER_A);
        assertThat(summary.activeRestrictionId()).isEqualTo(restrictionId);
    }

    @Test
    void unknownAlertsAreRejected() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                supervision.resolveAlert(UUID.randomUUID(), FerchoAlertDecision.DISMISS, ADMIN, null, null, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static void assertNotNull(Object value) {
        org.assertj.core.api.Assertions.assertThat(value).isNotNull();
    }
}
