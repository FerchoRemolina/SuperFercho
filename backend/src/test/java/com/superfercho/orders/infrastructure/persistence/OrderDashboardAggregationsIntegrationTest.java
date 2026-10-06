package com.superfercho.orders.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.superfercho.orders.application.dto.AdminBusinessPeriodRow;
import com.superfercho.orders.application.dto.AdminSalesBucketRow;
import com.superfercho.orders.application.port.OrderRepository;
import com.superfercho.platform.time.BucketGranularity;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
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

    @BeforeEach
    void cleanOrdersBetweenTests() {
        jdbcTemplate.update("delete from orders.order_items");
        jdbcTemplate.update("delete from orders.orders");
        jdbcTemplate.update("delete from payments.payments");
    }

    @Test
    void shouldRecognizeSalesOnlyForDeliveredOrdersByDeliveredAt() {
        UUID customerA = UUID.randomUUID();
        UUID customerB = UUID.randomUUID();
        UUID productA = UUID.randomUUID();
        UUID productB = UUID.randomUUID();

        // CONFIRMED: no es venta (aunque created_at cae en mayo).
        insertOrder("ORD-DASH-1", customerA, "CONFIRMED", MAY_START, null, items ->
                items.accept(productA, "Leche entera", 2, "5000.00"));
        // DELIVERED entregada el 15 may: venta del 15 (no del created_at).
        insertOrder("ORD-DASH-2", customerA, "DELIVERED", Instant.parse("2026-05-01T20:00:00Z"),
                Instant.parse("2026-05-15T20:00:00Z"),
                items -> items.accept(productA, "Leche entera", 3, "8000.00"));
        // CANCELLED: nunca es venta.
        insertOrder("ORD-DASH-3", customerA, "CANCELLED", Instant.parse("2026-05-20T12:00:00Z"), null,
                items -> items.accept(productB, "Café", 9, "11000.00"));
        // Creada 31 may 23:59 Bogota, entregada DESPUÉS del corte: venta fuera de mayo.
        insertOrder("ORD-DASH-5", customerB, "DELIVERED", JUNE_START, JUNE_START,
                items -> items.accept(productB, "Café", 2, "3500.00"));

        // Ventas diarias por delivered_at: solo la entrega del 15 de mayo.
        List<AdminSalesBucketRow> daily =
                orderRepository.aggregateSalesBuckets(BucketGranularity.DAY, MAY_START, JUNE_START);
        assertThat(daily).hasSize(1);
        assertThat(daily.get(0).bucketStart()).isEqualTo(Instant.parse("2026-05-15T05:00:00Z"));
        assertThat(daily.get(0).totalAmount()).isEqualByComparingTo("24000.00");
        assertThat(daily.get(0).orderCount()).isEqualTo(1);

        // Resumen de negocio: ventas solo DELIVERED; cancelado por cancelled_at;
        // en proceso es el conteo actual (CONFIRMED de esta data).
        AdminBusinessPeriodRow summary =
                orderRepository.summarizeBusinessPeriod(MAY_START, JUNE_START);
        assertThat(summary.deliveredOrders()).isEqualTo(1);
        assertThat(summary.cancelledOrders()).isEqualTo(1);
        assertThat(summary.salesAmount()).isEqualByComparingTo("24000.00");
        assertThat(summary.inProcessOrders()).isEqualTo(1);

        // rankings (created_at, no cancelados): sin cambios de semántica.
        // ORD-DASH-5 (created = to, exclusivo) no entra en created_at.
        var topProducts = orderRepository.findTopProductsByQuantity(MAY_START, JUNE_START, 5, false);
        assertThat(topProducts).hasSize(1);
        assertThat(topProducts.get(0).productId()).isEqualTo(productA);
        assertThat(topProducts.get(0).quantity()).isEqualTo(5); // 2 + 3 unidades

        var topCustomers = orderRepository.findTopCustomersByTotal(MAY_START, JUNE_START, 5);
        assertThat(topCustomers).hasSize(1);
        assertThat(topCustomers.get(0).customerId()).isEqualTo(customerA);
        assertThat(topCustomers.get(0).totalAmount()).isEqualByComparingTo("34000.00");
        assertThat(topCustomers.get(0).orderCount()).isEqualTo(2);
    }

    @Test
    void shouldRecognizeSaleOnDeliveryDateNotCreationDate() {
        UUID customer = UUID.randomUUID();
        UUID product = UUID.randomUUID();

        // Creada domingo 31 may (17:00 Bogota), entregada lunes 1 jun (10:00 Bogota):
        // la venta pertenece al bucket del LUNES, aunque created_at sea domingo.
        insertOrder("ORD-DASH-WEEK", customer, "DELIVERED", Instant.parse("2026-05-31T22:00:00Z"),
                Instant.parse("2026-06-01T15:00:00Z"),
                items -> items.accept(product, "Panela", 1, "12000.00"));

        List<AdminSalesBucketRow> daily =
                orderRepository.aggregateSalesBuckets(BucketGranularity.DAY, JUNE_START, JULY_START);
        assertThat(daily).hasSize(1);
        assertThat(daily.get(0).bucketStart()).isEqualTo(JUNE_START);
        assertThat(daily.get(0).totalAmount()).isEqualByComparingTo("12000.00");
        assertThat(daily.get(0).orderCount()).isEqualTo(1);

        // La semana anterior (lunes 25 may → lunes 1 jun) NO incluye la venta.
        List<AdminSalesBucketRow> previousWeek =
                orderRepository.aggregateSalesBuckets(
                        BucketGranularity.DAY,
                        Instant.parse("2026-05-25T05:00:00Z"),
                        JUNE_START);
        assertThat(previousWeek).isEmpty();

        AdminBusinessPeriodRow summary =
                orderRepository.summarizeBusinessPeriod(JUNE_START, JULY_START);
        assertThat(summary.deliveredOrders()).isEqualTo(1);
        assertThat(summary.salesAmount()).isEqualByComparingTo("12000.00");
    }

    @Test
    void shouldCountCashOnDeliveryPendingOnceDelivered() {
        UUID customer = UUID.randomUUID();
        UUID product = UUID.randomUUID();

        // COD con pago PENDING: NO es venta hasta entregar. Aquí está DELIVERED.
        UUID orderId = insertOrder("ORD-DASH-COD", customer, "DELIVERED",
                Instant.parse("2026-07-10T15:00:00Z"), Instant.parse("2026-07-10T18:00:00Z"),
                items -> items.accept(product, "Arroz", 2, "5000.00"));
        insertPayment(orderId, "10000.00", "CASH_ON_DELIVERY", "PENDING");

        List<AdminSalesBucketRow> daily =
                orderRepository.aggregateSalesBuckets(BucketGranularity.DAY, JULY_START, AUGUST_START);
        assertThat(daily).hasSize(1);
        assertThat(daily.get(0).bucketStart()).isEqualTo(Instant.parse("2026-07-10T05:00:00Z"));
        assertThat(daily.get(0).totalAmount()).isEqualByComparingTo("10000.00");
        assertThat(daily.get(0).orderCount()).isEqualTo(1);

        AdminBusinessPeriodRow summary =
                orderRepository.summarizeBusinessPeriod(JULY_START, AUGUST_START);
        assertThat(summary.salesAmount()).isEqualByComparingTo("10000.00");
        assertThat(summary.deliveredOrders()).isEqualTo(1);
    }

    @Test
    void shouldNotCountUndeliveredOrdersAsSales() {
        UUID customer = UUID.randomUUID();
        UUID product = UUID.randomUUID();

        // CONFIRMED, PREPARING y DELIVERY con delivered_at null: $0 en ventas.
        insertOrder("ORD-DASH-C1", customer, "CONFIRMED", JULY_START, null,
                items -> items.accept(product, "Arroz", 1, "5000.00"));
        insertOrder("ORD-DASH-C2", customer, "PREPARING", JULY_START, null,
                items -> items.accept(product, "Arroz", 2, "5000.00"));
        insertOrder("ORD-DASH-C3", customer, "DELIVERY", JULY_START, null,
                items -> items.accept(product, "Arroz", 3, "5000.00"));

        List<AdminSalesBucketRow> daily =
                orderRepository.aggregateSalesBuckets(BucketGranularity.DAY, JULY_START, AUGUST_START);
        assertThat(daily).isEmpty();

        AdminBusinessPeriodRow summary =
                orderRepository.summarizeBusinessPeriod(JULY_START, AUGUST_START);
        assertThat(summary.salesAmount()).isEqualByComparingTo("0.00");
        assertThat(summary.deliveredOrders()).isEqualTo(0);
        assertThat(summary.inProcessOrders()).isEqualTo(3);
    }

    @Test
    void shouldRankLeastSoldProductsWithLimit() {
        UUID customer = UUID.randomUUID();
        UUID productHigh = UUID.randomUUID();
        UUID productLow = UUID.randomUUID();
        Instant from = JULY_START;
        Instant to = AUGUST_START;

        insertOrder("ORD-DASH-6", customer, "DELIVERED", Instant.parse("2026-07-05T15:00:00Z"),
                Instant.parse("2026-07-06T15:00:00Z"),
                items -> items.accept(productHigh, "Arroz", 4, "1000.00"));
        insertOrder("ORD-DASH-7", customer, "DELIVERED", Instant.parse("2026-07-10T15:00:00Z"),
                Instant.parse("2026-07-11T15:00:00Z"),
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

    private UUID insertOrder(
            String orderNumber,
            UUID customerId,
            String status,
            Instant createdAt,
            Instant deliveredAt,
            Consumer<ItemCollector> items) {
        UUID orderId = UUID.randomUUID();
        boolean cancelled = "CANCELLED".equals(status);
        jdbcTemplate.update(
                """
                INSERT INTO orders.orders (id, order_number, customer_id, status,
                    subtotal_amount, subtotal_currency, total_amount, total_currency, payment_id,
                    shipping_recipient_name, shipping_address_line, shipping_additional_info,
                    shipping_city, shipping_department, shipping_phone,
                    created_at, confirmed_at, cancelled_at, delivered_at, updated_at)
                VALUES (?, ?, ?, ?, 0, 'COP', 0, 'COP', NULL,
                    'Ada Lovelace', 'Calle 1 # 2-3', NULL,
                    'Bogotá', 'Cundinamarca', '3001234567',
                    ?, ?, ?, ?, ?)
                """,
                orderId,
                orderNumber,
                customerId,
                status,
                timestamp(createdAt),
                cancelled ? null : timestamp(createdAt),
                cancelled ? timestamp(createdAt) : null,
                deliveredAt == null ? null : timestamp(deliveredAt),
                timestamp(deliveredAt == null ? createdAt : deliveredAt));
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
        return orderId;
    }

    private void insertPayment(
            UUID orderId, String amount, String method, String status) {
        jdbcTemplate.update(
                """
                INSERT INTO payments.payments (id, order_id, amount, currency, payment_method,
                    status, provider_reference, created_at, updated_at)
                VALUES (?, ?, ?, 'COP', ?, ?, 'demo', ?, ?)
                """,
                UUID.randomUUID(),
                orderId,
                new BigDecimal(amount),
                method,
                status,
                timestamp(Instant.parse("2026-07-10T15:00:00Z")),
                timestamp(Instant.parse("2026-07-10T15:00:00Z")));
    }

    @FunctionalInterface
    private interface ItemCollector {
        void accept(UUID productId, String productName, int quantity, String unitPrice);
    }

    private static Timestamp timestamp(Instant instant) {
        return Timestamp.from(instant);
    }
}
