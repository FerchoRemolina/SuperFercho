package com.superfercho.identity.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.superfercho.identity.application.dto.AdminCustomerRecordListItem;
import com.superfercho.identity.application.dto.AdminCustomerRecordStatus;
import com.superfercho.identity.application.dto.AdminCustomerRecordSearchCriteria;
import com.superfercho.identity.application.dto.AdminCustomerRecordsPage;
import com.superfercho.identity.application.dto.AdminCustomerRecordSortBy;
import com.superfercho.identity.application.dto.AdminCustomerRecordSortDir;
import com.superfercho.identity.application.dto.AdminCustomerRecordStatusFilter;
import com.superfercho.identity.application.port.CustomerRecordRepository;
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
class AdminCustomerRecordsSearchIntegrationTest {

    private static final Instant BASE = Instant.parse("2026-08-01T05:00:00Z");

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

    private final java.util.concurrent.atomic.AtomicLong sequence = new java.util.concurrent.atomic.AtomicLong(System.currentTimeMillis());

    private void seedRecord(
            UUID id, String firstName, String lastName, String documentNumber) {
        String uniqueDoc = documentNumber + "-" + sequence.getAndIncrement();
        jdbcTemplate.update(
                """
                INSERT INTO identity.customer_records
                       (id, document_type, document_number, billing_first_name,
                        billing_last_name, created_at, updated_at)
                VALUES (?, 'CC', ?, ?, ?, ?, ?)
                """,
                id, uniqueDoc, firstName, lastName,
                Timestamp.from(BASE), Timestamp.from(BASE));
    }

    private void seedUser(
            UUID id, UUID recordId, String email, String status, boolean deleted, Instant createdAt) {
        String uniqueEmail = id.toString().substring(0, 8) + "-" + email;
        jdbcTemplate.update(
                """
                INSERT INTO identity.users
                       (id, first_name, last_name, email, phone, password_hash, role, status,
                        deleted_at, customer_record_id, created_at, updated_at)
                VALUES (?, 'Nombre', 'Apellido', ?, '3000000000', 'hash', 'CUSTOMER', ?, ?, ?, ?, ?)
                """,
                id, uniqueEmail, status, deleted ? Timestamp.from(BASE) : null,
                recordId, Timestamp.from(createdAt), Timestamp.from(createdAt));
    }

    private void seedOrder(
            UUID id, UUID customerId, double total, Instant createdAt) {
        jdbcTemplate.update(
                """
                INSERT INTO orders.orders
                       (id, order_number, customer_id, status, subtotal_amount, subtotal_currency,
                        total_amount, total_currency, shipping_recipient_name, shipping_address_line,
                        shipping_city, shipping_department, shipping_phone, created_at, updated_at)
                VALUES (?, ?, ?, 'DELIVERED', ?, 'COP', ?, 'COP', 'Cliente', 'Calle 1',
                        'Bogotá', 'Cundinamarca', '3000000000', ?, ?)
                """,
                id, "ORD-" + id.toString().substring(0, 8), customerId,
                new java.math.BigDecimal(String.valueOf(total)),
                new java.math.BigDecimal(String.valueOf(total)),
                Timestamp.from(createdAt), Timestamp.from(createdAt));
    }

    @Test
    void shouldListWithoutOptionalFiltersMatchingDefaultAdminPage() {
        // Mirrors GET /api/v1/admin/customers?page=0&size=20 (no search/status/hasPurchases).
        // hasPurchases=null must not trigger PostgreSQL 42P18 (untyped JDBC null).
        UUID id = UUID.randomUUID();
        seedRecord(id, "Ana", "Gómez", "1020304050");
        seedUser(UUID.randomUUID(), id, "ana@example.com", "ACTIVE", false, BASE);

        AdminCustomerRecordsPage page = customerRecordRepository.searchRecords(
                new AdminCustomerRecordSearchCriteria(
                        "", AdminCustomerRecordStatusFilter.ALL,
                        AdminCustomerRecordSortBy.CREATED_AT,
                        AdminCustomerRecordSortDir.DESC, null, 0, 20));

        assertThat(page.page()).isZero();
        assertThat(page.size()).isEqualTo(20);
        assertThat(page.totalElements()).isGreaterThanOrEqualTo(1);
        AdminCustomerRecordListItem item = page.items().stream()
                .filter(row -> row.id().equals(id))
                .findFirst()
                .orElseThrow();
        assertThat(item.status()).isEqualTo(AdminCustomerRecordStatus.ACTIVE);
        assertThat(item.email()).contains("ana@example.com");
        assertThat(item.orderCount()).isZero();
    }

    @Test
    void shouldReturnAllRecordsWhenStatusIsAll() {
        UUID id = UUID.randomUUID();
        seedRecord(id, "Ana", "Gómez", "1020304050");
        seedUser(UUID.randomUUID(), id, "ana@example.com", "ACTIVE", false, BASE);

        AdminCustomerRecordsPage page = customerRecordRepository.searchRecords(
                new AdminCustomerRecordSearchCriteria(
                        "", AdminCustomerRecordStatusFilter.ALL,
                        AdminCustomerRecordSortBy.CREATED_AT,
                        AdminCustomerRecordSortDir.DESC, null, 0, 20));

        assertThat(page.totalElements()).isGreaterThanOrEqualTo(1);
        AdminCustomerRecordListItem item = page.items().stream()
                .filter(row -> row.id().equals(id))
                .findFirst()
                .orElseThrow();
        assertThat(item.status()).isEqualTo(AdminCustomerRecordStatus.ACTIVE);
        assertThat(item.email()).contains("ana@example.com");
    }

    @Test
    void shouldDeriveAndFilterRecordStatusesDisjointly() {
        String marker = "StatusSem" + sequence.getAndIncrement();

        UUID activeId = UUID.randomUUID();
        seedRecord(activeId, "Activa", marker, "1020304050");
        seedUser(UUID.randomUUID(), activeId, "activa@example.com", "ACTIVE", false, BASE);

        UUID inactiveId = UUID.randomUUID();
        seedRecord(inactiveId, "Inactiva", marker, "1020304051");
        seedUser(UUID.randomUUID(), inactiveId, "inactiva@example.com", "INACTIVE", false, BASE);

        UUID closedId = UUID.randomUUID();
        seedRecord(closedId, "Cerrada", marker, "1020304052");
        seedUser(UUID.randomUUID(), closedId, "cerrada@example.com", "ACTIVE", true, BASE);

        UUID noAccountId = UUID.randomUUID();
        seedRecord(noAccountId, "SinCuenta", marker, "1020304053");

        UUID activePlusDeletedId = UUID.randomUUID();
        seedRecord(activePlusDeletedId, "ActivaMix", marker, "1020304054");
        seedUser(UUID.randomUUID(), activePlusDeletedId, "mix-active@example.com", "ACTIVE", false, BASE);
        seedUser(UUID.randomUUID(), activePlusDeletedId, "mix-deleted-a@example.com", "ACTIVE", true, BASE);

        UUID inactivePlusDeletedId = UUID.randomUUID();
        seedRecord(inactivePlusDeletedId, "InactivaMix", marker, "1020304055");
        seedUser(UUID.randomUUID(), inactivePlusDeletedId, "mix-inactive@example.com", "INACTIVE", false, BASE);
        seedUser(UUID.randomUUID(), inactivePlusDeletedId, "mix-deleted-i@example.com", "ACTIVE", true, BASE);

        assertThat(statusOf(marker, activeId)).isEqualTo(AdminCustomerRecordStatus.ACTIVE);
        assertThat(statusOf(marker, inactiveId)).isEqualTo(AdminCustomerRecordStatus.INACTIVE);
        assertThat(statusOf(marker, closedId)).isEqualTo(AdminCustomerRecordStatus.CLOSED);
        assertThat(statusOf(marker, noAccountId)).isEqualTo(AdminCustomerRecordStatus.NO_ACCOUNT);
        assertThat(statusOf(marker, activePlusDeletedId)).isEqualTo(AdminCustomerRecordStatus.ACTIVE);
        assertThat(statusOf(marker, inactivePlusDeletedId)).isEqualTo(AdminCustomerRecordStatus.INACTIVE);

        var allPage = searchByStatus(marker, AdminCustomerRecordStatusFilter.ALL);
        assertThat(allPage.items()).extracting(AdminCustomerRecordListItem::id)
                .containsExactlyInAnyOrder(
                        activeId, inactiveId, closedId, noAccountId,
                        activePlusDeletedId, inactivePlusDeletedId);

        var activePage = searchByStatus(marker, AdminCustomerRecordStatusFilter.ACTIVE);
        assertThat(activePage.items()).extracting(AdminCustomerRecordListItem::id)
                .containsExactlyInAnyOrder(activeId, activePlusDeletedId);

        var inactivePage = searchByStatus(marker, AdminCustomerRecordStatusFilter.INACTIVE);
        assertThat(inactivePage.items()).extracting(AdminCustomerRecordListItem::id)
                .containsExactlyInAnyOrder(inactiveId, inactivePlusDeletedId);

        var closedPage = searchByStatus(marker, AdminCustomerRecordStatusFilter.CLOSED);
        assertThat(closedPage.items()).extracting(AdminCustomerRecordListItem::id)
                .containsExactly(closedId);

        var noAccountPage = searchByStatus(marker, AdminCustomerRecordStatusFilter.NO_ACCOUNT);
        assertThat(noAccountPage.items()).extracting(AdminCustomerRecordListItem::id)
                .containsExactly(noAccountId);
    }

    private AdminCustomerRecordStatus statusOf(String marker, UUID recordId) {
        return searchByStatus(marker, AdminCustomerRecordStatusFilter.ALL).items().stream()
                .filter(item -> item.id().equals(recordId))
                .map(AdminCustomerRecordListItem::status)
                .findFirst()
                .orElseThrow();
    }

    private AdminCustomerRecordsPage searchByStatus(
            String marker, AdminCustomerRecordStatusFilter status) {
        return customerRecordRepository.searchRecords(
                new AdminCustomerRecordSearchCriteria(
                        marker, status,
                        AdminCustomerRecordSortBy.CREATED_AT,
                        AdminCustomerRecordSortDir.DESC, null, 0, 20));
    }

    @Test
    void shouldSearchByNameFragment() {
        UUID id = UUID.randomUUID();
        seedRecord(id, "Mariana", "Restrepo", "1090888777");
        seedUser(UUID.randomUUID(), id, "mariana@example.com", "ACTIVE", false, BASE);

        var page = customerRecordRepository.searchRecords(
                new AdminCustomerRecordSearchCriteria(
                        "restrepo", AdminCustomerRecordStatusFilter.ALL,
                        AdminCustomerRecordSortBy.CREATED_AT,
                        AdminCustomerRecordSortDir.DESC, null, 0, 20));

        assertThat(page.items()).anyMatch(item -> item.id().equals(id));
    }

    @Test
    void shouldPaginateWithTotalElements() {
        for (int i = 0; i < 3; i++) {
            UUID id = UUID.randomUUID();
            seedRecord(id, "Paginada" + i, "Cliente", "102030406" + i);
        }
        var page = customerRecordRepository.searchRecords(
                new AdminCustomerRecordSearchCriteria(
                        "Paginada", AdminCustomerRecordStatusFilter.ALL,
                        AdminCustomerRecordSortBy.CREATED_AT,
                        AdminCustomerRecordSortDir.DESC, null, 0, 2));
        assertThat(page.items()).hasSize(2);
        assertThat(page.totalElements()).isEqualTo(3);
    }

    @Test
    void shouldSortByNameAscending() {
        String marker = "SortName" + sequence.getAndIncrement();
        UUID a = UUID.randomUUID();
        seedRecord(a, "Zoila", marker, "1020304090");
        UUID b = UUID.randomUUID();
        seedRecord(b, "Adela", marker, "1020304091");

        var page = customerRecordRepository.searchRecords(
                new AdminCustomerRecordSearchCriteria(
                        marker, AdminCustomerRecordStatusFilter.ALL,
                        AdminCustomerRecordSortBy.NAME,
                        AdminCustomerRecordSortDir.ASC, null, 0, 20));

        assertThat(page.items()).extracting(AdminCustomerRecordListItem::id)
                .containsExactly(b, a);
    }

    @Test
    void shouldRankByOrderCountAndExposeCommercialMetrics() {
        UUID recordId = UUID.randomUUID();
        seedRecord(recordId, "Comprador", "Uno", "1020304095");
        UUID userId = UUID.randomUUID();
        seedUser(userId, recordId, "comprador@example.com", "ACTIVE", false, BASE);
        seedOrder(UUID.randomUUID(), userId, 100, BASE);
        seedOrder(UUID.randomUUID(), userId, 200, BASE.plusSeconds(3600));

        var page = customerRecordRepository.searchRecords(
                new AdminCustomerRecordSearchCriteria(
                        "", AdminCustomerRecordStatusFilter.ALL,
                        AdminCustomerRecordSortBy.ORDERS,
                        AdminCustomerRecordSortDir.DESC, null, 0, 20));

        AdminCustomerRecordListItem item = page.items().stream()
                .filter(row -> row.id().equals(recordId))
                .findFirst()
                .orElseThrow();
        assertThat(item.orderCount()).isEqualTo(2);
        assertThat(item.totalSpent().amount().intValue()).isEqualTo(300);
        assertThat(item.lastOrderAt()).isNotNull();
    }

    @Test
    void shouldFilterCustomersWithoutPurchases() {
        UUID buyerId = UUID.randomUUID();
        seedRecord(buyerId, "Con", "Compras", "1020304100");
        UUID buyerUser = UUID.randomUUID();
        seedUser(buyerUser, buyerId, "buyer@example.com", "ACTIVE", false, BASE);
        seedOrder(UUID.randomUUID(), buyerUser, 50, BASE);

        UUID idleId = UUID.randomUUID();
        seedRecord(idleId, "Sin", "Compras", "1020304101");
        seedUser(UUID.randomUUID(), idleId, "idle@example.com", "ACTIVE", false, BASE);

        var withoutPurchases = customerRecordRepository.searchRecords(
                new AdminCustomerRecordSearchCriteria(
                        "", AdminCustomerRecordStatusFilter.ALL,
                        AdminCustomerRecordSortBy.CREATED_AT,
                        AdminCustomerRecordSortDir.DESC, Boolean.FALSE, 0, 50));

        assertThat(withoutPurchases.items()).anyMatch(item -> item.id().equals(idleId));
        assertThat(withoutPurchases.items()).noneMatch(item -> item.id().equals(buyerId));
    }

    @Test
    void shouldFilterCustomersWithPurchases() {
        UUID buyerId = UUID.randomUUID();
        seedRecord(buyerId, "Con", "Pedidos", "1020304110");
        UUID buyerUser = UUID.randomUUID();
        seedUser(buyerUser, buyerId, "with-orders@example.com", "ACTIVE", false, BASE);
        seedOrder(UUID.randomUUID(), buyerUser, 75, BASE);

        UUID idleId = UUID.randomUUID();
        seedRecord(idleId, "Sin", "Pedidos", "1020304111");
        seedUser(UUID.randomUUID(), idleId, "no-orders@example.com", "ACTIVE", false, BASE);

        var withPurchases = customerRecordRepository.searchRecords(
                new AdminCustomerRecordSearchCriteria(
                        "", AdminCustomerRecordStatusFilter.ALL,
                        AdminCustomerRecordSortBy.CREATED_AT,
                        AdminCustomerRecordSortDir.DESC, Boolean.TRUE, 0, 50));

        assertThat(withPurchases.items()).anyMatch(item -> item.id().equals(buyerId));
        assertThat(withPurchases.items()).noneMatch(item -> item.id().equals(idleId));
    }

    @Test
    void shouldSortByCreatedAtDescendingAsDefaultListPerspective() {
        String marker = "SortCreated" + sequence.getAndIncrement();
        Instant olderAt = BASE;
        Instant newerAt = BASE.plusSeconds(7200);

        UUID older = UUID.randomUUID();
        seedRecord(older, "Vieja", marker, "1020304120");
        jdbcTemplate.update(
                """
                UPDATE identity.customer_records
                   SET created_at = ?, updated_at = ?
                 WHERE id = ?
                """,
                Timestamp.from(olderAt), Timestamp.from(olderAt), older);

        UUID newer = UUID.randomUUID();
        seedRecord(newer, "Nueva", marker, "1020304121");
        jdbcTemplate.update(
                """
                UPDATE identity.customer_records
                   SET created_at = ?, updated_at = ?
                 WHERE id = ?
                """,
                Timestamp.from(newerAt), Timestamp.from(newerAt), newer);

        var page = customerRecordRepository.searchRecords(
                new AdminCustomerRecordSearchCriteria(
                        marker, AdminCustomerRecordStatusFilter.ALL,
                        AdminCustomerRecordSortBy.CREATED_AT,
                        AdminCustomerRecordSortDir.DESC, null, 0, 20));

        assertThat(page.items()).extracting(AdminCustomerRecordListItem::id)
                .containsExactly(newer, older);
    }
}
