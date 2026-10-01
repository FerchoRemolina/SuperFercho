package com.superfercho.orders.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.superfercho.orders.application.dto.AdminSalesBucketRow;
import com.superfercho.orders.application.port.OrderRepository;
import com.superfercho.platform.time.BucketGranularity;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
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
class OrderDashboardAggregationsIntegrationTest {

    private static final Instant MAY_START = Instant.parse("2026-05-01T05:00:00Z");
    private static final Instant JUNE_START = Instant.parse("2026-06-01T05:00:00Z");
    private static final Instant JULY_START = Instant.parse("2026-07-01T05:00:00Z");
    private static final Instant AUGUST_START = Instant.parse("2026-08-01T05:00:00Z");

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
    private OrderRepository orderRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldAggregateSalesStatusesProductsAndCustomersOverBogotaMonth() {
        UUID customerA = UUID.randomUUID();
        UUID customerB = UUID.randomUUID();
        UUID productA = UUID.randomUUID();
        UUID productB = UUID.randomUUID();

        // from exacto (inclusive): 1 may 00:00 Bogota
        insertOrder("ORD-DASH-1", customerA, "CONFIRMED", MAY_START, items ->
                items.accept(productA, "Leche entera", 2, "5000.00"));
        // mitad del mes
        insertOrder("ORD-DASH-2", customerA, "DELIVERED", Instant.parse("2026-05-15T20:00:00Z"),
                items -> items.accept(productA, "Leche entera", 3, "8000.00"));
        // cancelado: no cuenta en ventas, productos ni clientes
        insertOrder("ORD-DASH-3", customerA, "CANCELLED", Instant.parse("2026-05-20T12:00:00Z"),
                items -> items.accept(productB, "Café", 9, "11000.00"));
        // último instante de mayo en Bogota (31 may 23:59:59)
        insertOrder("ORD-DASH-4", customerB, "CONFIRMED", Instant.parse("2026-06-01T04:59:59Z"),
                items -> items.accept(productB, "Café", 1, "5000.00"));
        // to exacto (exclusivo): no debe contar
        insertOrder("ORD-DASH-5", customerB, "CONFIRMED", JUNE_START,
                items -> items.accept(productB, "Café", 2, "3500.00"));

        // ventas por día (America/Bogota): CANCELLED excluido
        List<AdminSalesBucketRow> daily =
                orderRepository.aggregateSalesBuckets(BucketGranularity.DAY, MAY_START, JUNE_START);
        assertThat(daily).hasSize(3);
        assertThat(daily.get(0).bucketStart()).isEqualTo(MAY_START);
        assertThat(daily.get(0).totalAmount()).isEqualByComparingTo("10000.00");
        assertThat(daily.get(0).orderCount()).isEqualTo(1);
        assertThat(daily.get(2).bucketStart()).isEqualTo(Instant.parse("2026-05-31T05:00:00Z"));
        assertThat(daily.get(2).totalAmount()).isEqualByComparingTo("5000.00");

        // ventas por mes: un único bucket de mayo con 3 pedidos no cancelados.
        // Si `to` fuera inclusivo, ORD-DASH-5 sumaría 7000 y el total sería 46000.
        List<AdminSalesBucketRow> monthly =
                orderRepository.aggregateSalesBuckets(BucketGranularity.MONTH, MAY_START, JUNE_START);
        assertThat(monthly).hasSize(1);
        assertThat(monthly.get(0).bucketStart()).isEqualTo(MAY_START);
        assertThat(monthly.get(0).totalAmount()).isEqualByComparingTo("39000.00");
        assertThat(monthly.get(0).orderCount()).isEqualTo(3);

        // contadores por estado dentro del rango
        var statusRows = orderRepository.countByStatusBetween(MAY_START, JUNE_START);
        assertThat(statusRows)
                .extracting(row -> row.status().name() + ":" + row.orderCount())
                .containsExactlyInAnyOrder("CONFIRMED:2", "DELIVERED:1", "CANCELLED:1");
        assertThat(statusRows)
                .filteredOn(row -> row.status().name().equals("CANCELLED"))
                .allSatisfy(row -> assertThat(row.totalAmount()).isEqualByComparingTo("99000.00"));

        // ranking de productos por unidades vendidas (CANCELLED excluido)
        var topProducts = orderRepository.findTopProductsByQuantity(MAY_START, JUNE_START, 5, false);
        assertThat(topProducts).hasSize(2);
        assertThat(topProducts.get(0).productId()).isEqualTo(productA);
        assertThat(topProducts.get(0).quantity()).isEqualTo(5); // 2 + 3 unidades
        assertThat(topProducts.get(1).productId()).isEqualTo(productB);
        assertThat(topProducts.get(1).quantity()).isEqualTo(1); // sin las 9 del cancelado

        // top clientes por valor comprado (CANCELLED excluido)
        var topCustomers = orderRepository.findTopCustomersByTotal(MAY_START, JUNE_START, 5);
        assertThat(topCustomers).hasSize(2);
        assertThat(topCustomers.get(0).customerId()).isEqualTo(customerA);
        assertThat(topCustomers.get(0).totalAmount()).isEqualByComparingTo("34000.00");
        assertThat(topCustomers.get(0).orderCount()).isEqualTo(2);
        assertThat(topCustomers.get(1).customerId()).isEqualTo(customerB);
        assertThat(topCustomers.get(1).totalAmount()).isEqualByComparingTo("5000.00");
    }

    @Test
    void shouldRankLeastSoldProductsWithLimit() {
        UUID customer = UUID.randomUUID();
        UUID productHigh = UUID.randomUUID();
        UUID productLow = UUID.randomUUID();
        Instant from = JULY_START;
        Instant to = AUGUST_START;

        insertOrder("ORD-DASH-6", customer, "DELIVERED", Instant.parse("2026-07-05T15:00:00Z"),
                items -> items.accept(productHigh, "Arroz", 4, "1000.00"));
        insertOrder("ORD-DASH-7", customer, "DELIVERED", Instant.parse("2026-07-10T15:00:00Z"),
                items -> items.accept(productLow, "Panela", 1, "2000.00"));

        var least = orderRepository.findTopProductsByQuantity(from, to, 1, true);
        assertThat(least).hasSize(1);
        assertThat(least.get(0).productId()).isEqualTo(productLow);
        assertThat(least.get(0).quantity()).isEqualTo(1);

        var most = orderRepository.findTopProductsByQuantity(from, to, 1, false);
        assertThat(most).hasSize(1);
        assertThat(most.get(0).productId()).isEqualTo(productHigh);
        assertThat(most.get(0).quantity()).isEqualTo(4);
    }

    private void insertOrder(
            String orderNumber,
            UUID customerId,
            String status,
            Instant createdAt,
            Consumer<ItemCollector> items) {
        UUID orderId = UUID.randomUUID();
        boolean cancelled = "CANCELLED".equals(status);
        jdbcTemplate.update(
                """
                INSERT INTO orders.orders (id, order_number, customer_id, status,
                    subtotal_amount, subtotal_currency, total_amount, total_currency, payment_id,
                    shipping_recipient_name, shipping_address_line, shipping_additional_info,
                    shipping_city, shipping_department, shipping_phone,
                    created_at, confirmed_at, cancelled_at, updated_at)
                VALUES (?, ?, ?, ?, 0, 'COP', 0, 'COP', NULL,
                    'Ada Lovelace', 'Calle 1 # 2-3', NULL,
                    'Bogotá', 'Cundinamarca', '3001234567',
                    ?, ?, ?, ?)
                """,
                orderId,
                orderNumber,
                customerId,
                status,
                timestamp(createdAt),
                cancelled ? null : timestamp(createdAt),
                cancelled ? timestamp(createdAt) : null,
                timestamp(createdAt));
        items.accept((productId, productName, quantity, unitPrice) -> {
            BigDecimal price = new BigDecimal(unitPrice);
            BigDecimal subtotal = price.multiply(BigDecimal.valueOf(quantity));
            jdbcTemplate.update(
                    """
                    INSERT INTO orders.order_items (id, order_id, item_index, product_id, product_name,
                        unit_price_amount, unit_price_currency, quantity, subtotal_amount, subtotal_currency)
                    VALUES (?, ?, 0, ?, ?, ?, 'COP', ?, ?, 'COP')
                    """,
                    UUID.randomUUID(),
                    orderId,
                    productId,
                    productName,
                    price,
                    quantity,
                    subtotal);
            jdbcTemplate.update(
                    "UPDATE orders.orders SET subtotal_amount = subtotal_amount + ?, total_amount = total_amount + ? WHERE id = ?",
                    subtotal,
                    subtotal,
                    orderId);
        });
    }

    @FunctionalInterface
    private interface ItemCollector {
        void accept(UUID productId, String productName, int quantity, String unitPrice);
    }

    private static Timestamp timestamp(Instant instant) {
        return Timestamp.from(instant);
    }
}
