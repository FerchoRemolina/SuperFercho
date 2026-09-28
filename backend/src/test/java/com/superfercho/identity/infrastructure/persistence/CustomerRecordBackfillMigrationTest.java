package com.superfercho.identity.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
class CustomerRecordBackfillMigrationTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(
                            DockerImageName.parse("pgvector/pgvector:pg16")
                                    .asCompatibleSubstituteFor("postgres"))
                    .withDatabaseName("superfercho")
                    .withUsername("superfercho")
                    .withPassword("superfercho");

    @Test
    void backfillsCommercialCustomersLeavesAdminAndPreviewWithoutRecord() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(POSTGRES.getJdbcUrl());
        dataSource.setUsername(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());
        dataSource.setDriverClassName("org.postgresql.Driver");

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .target("16")
                .load()
                .migrate();

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        UUID adminId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID previewId = UUID.randomUUID();
        String adminDoc = "8100000001";
        String customerDoc = "9100000001";
        String previewDoc = "PREV" + previewId.toString().replace("-", "").substring(0, 12);
        String adminEmail = "admin-backfill@example.com";
        String customerEmail = "customer-backfill@example.com";
        String previewEmail = "preview+" + previewId.toString().substring(0, 8) + "@temp.superfercho.local";

        insertUser(jdbc, adminId, "CC", adminDoc, "Admin", "User", adminEmail, "ADMIN");
        insertUser(jdbc, customerId, "CC", customerDoc, "Ada", "Lovelace", customerEmail, "CUSTOMER");
        insertUser(jdbc, previewId, "CC", previewDoc, "Preview", "Customer", previewEmail, "CUSTOMER");

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .target("17")
                .load()
                .migrate();

        Map<String, Object> admin = jdbc.queryForMap(
                "select customer_record_id, document_type, document_number, email, deleted_at from identity.users where id = ?",
                adminId);
        Map<String, Object> customer = jdbc.queryForMap(
                "select customer_record_id, document_type, document_number, email, deleted_at from identity.users where id = ?",
                customerId);
        Map<String, Object> preview = jdbc.queryForMap(
                "select customer_record_id, document_type, document_number, email from identity.users where id = ?",
                previewId);

        assertThat(admin.get("customer_record_id")).isNull();
        assertThat(admin.get("document_type")).isEqualTo("CC");
        assertThat(admin.get("document_number")).isEqualTo(adminDoc);
        assertThat(admin.get("email")).isEqualTo(adminEmail);
        assertThat(admin.get("deleted_at")).isNull();

        assertThat(customer.get("customer_record_id")).isNotNull();
        assertThat(customer.get("document_number")).isEqualTo(customerDoc);
        assertThat(customer.get("email")).isEqualTo(customerEmail);
        assertThat(customer.get("deleted_at")).isNull();

        assertThat(preview.get("customer_record_id")).isNull();
        assertThat(preview.get("document_number")).isEqualTo(previewDoc);

        Map<String, Object> record = jdbc.queryForMap(
                """
                select document_type, document_number, billing_first_name, billing_last_name
                  from identity.customer_records
                 where id = ?
                """,
                customer.get("customer_record_id"));
        assertThat(record.get("document_type")).isEqualTo("CC");
        assertThat(record.get("document_number")).isEqualTo(customerDoc);
        assertThat(record.get("billing_first_name")).isEqualTo("Ada");
        assertThat(record.get("billing_last_name")).isEqualTo("Lovelace");

        Integer commercialWithoutRecord = jdbc.queryForObject(
                """
                select count(*) from identity.users
                 where role = 'CUSTOMER'
                   and customer_record_id is null
                   and document_number not like 'PREV%'
                   and email not like 'preview+%@temp.superfercho.local'
                """,
                Integer.class);
        assertThat(commercialWithoutRecord).isZero();
    }

    private static void insertUser(
            JdbcTemplate jdbc,
            UUID id,
            String documentType,
            String documentNumber,
            String firstName,
            String lastName,
            String email,
            String role) {
        jdbc.update(
                """
                insert into identity.users (
                    id, document_type, document_number, first_name, last_name,
                    email, phone, password_hash, role, status, created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?, '3001234567', 'hash', ?, 'ACTIVE', ?, ?)
                """,
                id,
                documentType,
                documentNumber,
                firstName,
                lastName,
                email,
                role,
                Timestamp.from(NOW),
                Timestamp.from(NOW));
    }
}
