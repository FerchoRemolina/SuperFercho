package com.superfercho.identity.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.superfercho.identity.application.port.CustomerRecordRepository;
import com.superfercho.platform.time.BucketGranularity;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
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

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class CustomerRecordAggregationsIntegrationTest {

    private static final Instant MAY_START = Instant.parse("2026-05-01T05:00:00Z");
    private static final Instant JUNE_START = Instant.parse("2026-06-01T05:00:00Z");

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
    }

    @Autowired
    private CustomerRecordRepository customerRecordRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldCountRegistrationsByBucketFollowingBogotaDays() {
        insertRecord("2026-05-01T02:00:00Z"); // 30 abr 21:00 Bogota: fuera
        insertRecord("2026-05-01T05:00:00Z"); // from inclusivo: 1 may 00:00 Bogota
        insertRecord("2026-05-15T20:00:00Z");
        insertRecord("2026-06-01T04:59:59Z"); // 31 may 23:59:59 Bogota: dentro
        insertRecord("2026-06-01T05:00:00Z"); // to exclusivo: fuera

        long total = customerRecordRepository.countRegistrationsBetween(MAY_START, JUNE_START);
        assertThat(total).isEqualTo(3);

        var daily = customerRecordRepository.countRegistrationsByBucket(
                BucketGranularity.DAY, MAY_START, JUNE_START);
        assertThat(daily).hasSize(3);
        assertThat(daily.get(0).bucketStart()).isEqualTo(MAY_START);
        assertThat(daily.get(0).count()).isEqualTo(1);
        assertThat(daily.get(1).bucketStart()).isEqualTo(Instant.parse("2026-05-15T05:00:00Z"));
        assertThat(daily.get(1).count()).isEqualTo(1);
        assertThat(daily.get(2).bucketStart()).isEqualTo(Instant.parse("2026-05-31T05:00:00Z"));
        assertThat(daily.get(2).count()).isEqualTo(1);

        var monthly = customerRecordRepository.countRegistrationsByBucket(
                BucketGranularity.MONTH, MAY_START, JUNE_START);
        assertThat(monthly).hasSize(1);
        assertThat(monthly.get(0).bucketStart()).isEqualTo(MAY_START);
        assertThat(monthly.get(0).count()).isEqualTo(3);
    }

    private void insertRecord(String createdAt) {
        Instant instant = Instant.parse(createdAt);
        jdbcTemplate.update(
                """
                INSERT INTO identity.customer_records (id, document_type, document_number,
                    billing_first_name, billing_last_name, created_at, updated_at)
                VALUES (?, 'CC', ?, 'Ada', 'Lovelace', ?, ?)
                """,
                UUID.randomUUID(),
                "DASH-" + UUID.randomUUID().toString().substring(0, 12),
                timestamp(instant),
                timestamp(instant));
    }

    private static Timestamp timestamp(Instant instant) {
        return Timestamp.from(instant);
    }
}
