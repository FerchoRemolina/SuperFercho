package com.superfercho.orders.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.superfercho.orders.application.dto.PageRequest;
import com.superfercho.orders.application.port.OrderRepository;
import com.superfercho.orders.domain.model.Order;
import com.superfercho.orders.domain.model.OrderItem;
import com.superfercho.orders.domain.model.OrderNumber;
import com.superfercho.orders.domain.model.OrderStatus;
import com.superfercho.orders.domain.model.ShippingAddressSnapshot;
import com.superfercho.orders.infrastructure.persistence.repository.OrderJpaRepository;
import com.superfercho.platform.money.Money;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class OrderPersistenceAdapterTest {

    private static final Instant CREATED_AT = Instant.parse("2026-03-01T10:00:00Z");
    private static final Instant PREPARING_AT = Instant.parse("2026-03-01T10:02:00Z");
    private static final UUID CUSTOMER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID OTHER_CUSTOMER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID PRODUCT_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID SECOND_PRODUCT_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID PAYMENT_ID = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");

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
    private OrderJpaRepository orderJpaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldPersistAndReloadOrderById() {
        Order saved = orderRepository.save(confirmedOrder("ORD-P-1001", CUSTOMER_ID, PAYMENT_ID));

        Order loaded = orderRepository.findById(saved.id()).orElseThrow();

        assertThat(loaded.id()).isEqualTo(saved.id());
        assertThat(loaded.orderNumber().value()).isEqualTo("ORD-P-1001");
        assertThat(loaded.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(loaded.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(loaded.paymentId()).isEqualTo(PAYMENT_ID);
        assertThat(loaded.total()).isEqualTo(Money.cop(new BigDecimal("21.00")));
        assertThat(loaded.createdAt()).isEqualTo(CREATED_AT);
        assertThat(loaded.updatedAt()).isEqualTo(CREATED_AT);
        assertThat(loaded.confirmedAt()).isEqualTo(CREATED_AT);
        assertThat(loaded.cancelledAt()).isNull();
        assertThat(loaded.shippingAddress().recipientName()).isEqualTo("Ada Lovelace");
        assertThat(loaded.shippingAddress().addressLine()).isEqualTo("Calle 1 # 2-3");
        assertThat(loaded.shippingAddress().city()).isEqualTo("Bogotá");
        assertThat(loaded.shippingAddress().department()).isEqualTo("Cundinamarca");
        assertThat(loaded.shippingAddress().phone()).isEqualTo("3001234567");
        assertThat(loaded.items()).hasSize(1);
        assertThat(loaded.items().get(0).productName()).isEqualTo("Leche entera");
        assertThat(loaded.items().get(0).quantity()).isEqualTo(2);
        assertThat(loaded.items().get(0).unitPrice()).isEqualTo(Money.cop(new BigDecimal("10.50")));
    }

    @Test
    void shouldFindByOrderNumber() {
        Order saved = orderRepository.save(confirmedOrder("ORD-P-1002", CUSTOMER_ID, null));

        assertThat(orderJpaRepository.findByOrderNumber("ORD-P-1002"))
                .hasValueSatisfying(entity -> assertThat(entity.getId()).isEqualTo(saved.id()));
        assertThat(orderJpaRepository.findByOrderNumber("ORD-MISSING")).isEmpty();
        assertThat(saved.paymentId()).isNull();
        assertThat(orderRepository.findById(saved.id()).orElseThrow().paymentId()).isNull();
    }

    @Test
    void shouldFindAllOrdersAcrossCustomers() {
        UUID customerId = UUID.randomUUID();
        UUID otherCustomerId = UUID.randomUUID();
        Order own = orderRepository.save(confirmedOrder("ORD-P-ALL-1", customerId, PAYMENT_ID));
        Order other = orderRepository.save(confirmedOrder("ORD-P-ALL-2", otherCustomerId, PAYMENT_ID));

        assertThat(orderRepository.findAll(PageRequest.of(0, 20)).items())
                .extracting(Order::id)
                .contains(own.id(), other.id());
    }

    @Test
    void shouldFindOrdersByStatusesAtDatabaseWithCorrectTotalElements() {
        UUID customerId = UUID.randomUUID();
        Instant at = CREATED_AT.plusSeconds(60);
        Order confirmed = orderRepository.save(confirmedOrder("ORD-P-SALES-2", customerId, PAYMENT_ID));
        Order cancelled = orderRepository.save(
                confirmedOrder("ORD-P-SALES-3", customerId, PAYMENT_ID).cancel(CREATED_AT.plusSeconds(30)));
        Order delivered = orderRepository.save(confirmedOrder("ORD-P-SALES-4", customerId, PAYMENT_ID)
                .startPreparation(at)
                .startDelivery(at)
                .markDelivered(at));

        var sales = List.of(OrderStatus.CONFIRMED, OrderStatus.PREPARING, OrderStatus.DELIVERY, OrderStatus.DELIVERED);
        var allSales = orderRepository.findByStatuses(sales, PageRequest.of(0, 100));
        var firstPage = orderRepository.findByStatuses(sales, new PageRequest(0, 1));

        assertThat(allSales.items())
                .extracting(Order::id)
                .contains(confirmed.id(), delivered.id())
                .doesNotContain(cancelled.id());
        assertThat(allSales.totalElements()).isEqualTo(allSales.items().size());
        assertThat(firstPage.items()).hasSize(1);
        assertThat(firstPage.size()).isEqualTo(1);
        assertThat(firstPage.totalElements()).isEqualTo(allSales.totalElements());
        assertThat(firstPage.totalElements()).isGreaterThan(firstPage.items().size());
    }

    @Test
    void shouldFindOrdersByCustomerId() {
        UUID customerId = UUID.randomUUID();
        UUID otherCustomerId = UUID.randomUUID();
        Order own = orderRepository.save(confirmedOrder("ORD-P-1003", customerId, PAYMENT_ID));
        orderRepository.save(confirmedOrder("ORD-P-1004", otherCustomerId, PAYMENT_ID));

        assertThat(orderRepository.findByCustomerId(customerId, PageRequest.of(0, 20)).items())
                .extracting(Order::id)
                .containsExactly(own.id());
    }

    @Test
    void shouldPersistMultipleItemsInOrder() {
        Order saved = orderRepository.save(orderWithTwoItems("ORD-P-1005"));

        Order loaded = orderRepository.findById(saved.id()).orElseThrow();

        assertThat(loaded.items()).hasSize(2);
        assertThat(loaded.items().get(0).productName()).isEqualTo("Leche entera");
        assertThat(loaded.items().get(1).productName()).isEqualTo("Pan");
        assertThat(loaded.total()).isEqualTo(Money.cop(new BigDecimal("24.00")));
    }

    @Test
    void shouldPersistLifecycleStatusAndTimestamps() {
        Order saved = orderRepository.save(
                confirmedOrder("ORD-P-1006", CUSTOMER_ID, PAYMENT_ID).startPreparation(PREPARING_AT));

        Order loaded = orderRepository.findById(saved.id()).orElseThrow();

        assertThat(loaded.status()).isEqualTo(OrderStatus.PREPARING);
        assertThat(loaded.confirmedAt()).isEqualTo(CREATED_AT);
        assertThat(loaded.updatedAt()).isEqualTo(PREPARING_AT);
    }

    @Test
    void shouldRejectDuplicateOrderNumber() {
        orderRepository.save(confirmedOrder("ORD-P-DUP", CUSTOMER_ID, PAYMENT_ID));

        assertThatThrownBy(() -> orderRepository.save(confirmedOrder("ORD-P-DUP", OTHER_CUSTOMER_ID, PAYMENT_ID)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectInvalidQuantityAtDatabase() {
        UUID orderId = UUID.randomUUID();
        insertOrderHeader(orderId, "ORD-P-QTY");
        try {
            assertThatThrownBy(() -> insertItemBypassingDomain(orderId, 0, new BigDecimal("10.50"), "COP"))
                    .isInstanceOf(DataIntegrityViolationException.class);
        } finally {
            jdbcTemplate.update("delete from orders.orders where id = ?", orderId);
        }
    }

    @Test
    void shouldRejectNegativeMoneyAtDatabase() {
        UUID orderId = UUID.randomUUID();
        insertOrderHeader(orderId, "ORD-P-MONEY");
        try {
            assertThatThrownBy(() -> insertItemBypassingDomain(orderId, 1, new BigDecimal("-0.01"), "COP"))
                    .isInstanceOf(DataIntegrityViolationException.class);
        } finally {
            jdbcTemplate.update("delete from orders.orders where id = ?", orderId);
        }
    }

    @Test
    void shouldRejectInvalidStatusAtDatabase() {
        assertThatThrownBy(() -> insertOrderHeaderWithStatus(UUID.randomUUID(), "ORD-P-STATUS", "PAID"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectNonCopCurrencyAtDatabase() {
        UUID orderId = UUID.randomUUID();
        insertOrderHeader(orderId, "ORD-P-CUR");
        try {
            assertThatThrownBy(() -> insertItemBypassingDomain(orderId, 1, new BigDecimal("10.50"), "USD"))
                    .isInstanceOf(DataIntegrityViolationException.class);
        } finally {
            jdbcTemplate.update("delete from orders.orders where id = ?", orderId);
        }
    }

    @Test
    void shouldFindInProgressOrdersForLifecycle() {
        UUID customerId = UUID.randomUUID();
        Order confirmed = orderRepository.save(orderAt("ORD-P-AUTO-1", customerId, CREATED_AT));
        Order preparing = orderRepository.save(
                orderAt("ORD-P-AUTO-2", customerId, CREATED_AT).startPreparation(PREPARING_AT));
        Order delivery = orderRepository.save(orderAt("ORD-P-AUTO-3", customerId, CREATED_AT)
                .startPreparation(PREPARING_AT)
                .startDelivery(PREPARING_AT.plusSeconds(120)));
        Order delivered = orderRepository.save(orderAt("ORD-P-AUTO-4", customerId, CREATED_AT)
                .startPreparation(PREPARING_AT)
                .startDelivery(PREPARING_AT.plusSeconds(120))
                .markDelivered(PREPARING_AT.plusSeconds(240)));
        Order cancelled =
                orderRepository.save(orderAt("ORD-P-AUTO-5", customerId, CREATED_AT).cancel(CREATED_AT));

        List<Order> found = orderRepository.findInProgressForLifecycle();

        assertThat(found)
                .extracting(Order::id)
                .contains(confirmed.id(), preparing.id(), delivery.id())
                .doesNotContain(delivered.id(), cancelled.id());
        assertThat(found).allMatch(order -> order.confirmedAt() != null);
        assertThat(found)
                .allMatch(order -> order.status() == OrderStatus.CONFIRMED
                        || order.status() == OrderStatus.PREPARING
                        || order.status() == OrderStatus.DELIVERY);
    }

    @Test
    void shouldStartPreparationOnlyWhileStillConfirmed() {
        Order confirmed = orderRepository.save(confirmedOrder("ORD-P-PEND-1", CUSTOMER_ID, PAYMENT_ID));

        assertThat(orderRepository.saveIfCurrent(
                        confirmed.startPreparation(PREPARING_AT), OrderStatus.CONFIRMED))
                .hasValueSatisfying(order -> {
                    assertThat(order.status()).isEqualTo(OrderStatus.PREPARING);
                    assertThat(order.confirmedAt()).isEqualTo(CREATED_AT);
                    assertThat(order.cancelledAt()).isNull();
                    assertThat(order.deliveredAt()).isNull();
                });
        assertThat(orderRepository.saveIfCurrent(
                        confirmed.cancel(CREATED_AT.plusSeconds(30)), OrderStatus.CONFIRMED))
                .isEmpty();

        Order loaded = orderRepository.findById(confirmed.id()).orElseThrow();
        assertThat(loaded.status()).isEqualTo(OrderStatus.PREPARING);
        assertThat(loaded.cancelledAt()).isNull();
        assertThat(loaded.confirmedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void shouldCancelOrderOnlyWhileStillConfirmed() {
        Order confirmed = orderRepository.save(confirmedOrder("ORD-P-PEND-2", CUSTOMER_ID, PAYMENT_ID));
        Instant at = CREATED_AT.plusSeconds(30);

        assertThat(orderRepository.saveIfCurrent(confirmed.cancel(at), OrderStatus.CONFIRMED))
                .hasValueSatisfying(order -> {
                    assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);
                    assertThat(order.cancelledAt()).isEqualTo(at);
                    assertThat(order.confirmedAt()).isNull();
                    assertThat(order.deliveredAt()).isNull();
                });
        assertThat(orderRepository.saveIfCurrent(
                        confirmed.startPreparation(PREPARING_AT), OrderStatus.CONFIRMED))
                .isEmpty();

        Order loaded = orderRepository.findById(confirmed.id()).orElseThrow();
        assertThat(loaded.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(loaded.confirmedAt()).isNull();
        assertThat(loaded.cancelledAt()).isEqualTo(at);
    }

    @Test
    void shouldRejectSaveIfCurrentWhenStatusIsStillFromStatus() {
        Order confirmed = orderRepository.save(confirmedOrder("ORD-P-PEND-3", CUSTOMER_ID, PAYMENT_ID));

        assertThatThrownBy(() -> orderRepository.saveIfCurrent(confirmed, OrderStatus.CONFIRMED))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static Order confirmedOrder(String orderNumber, UUID customerId, UUID paymentId) {
        return orderAt(orderNumber, customerId, CREATED_AT, paymentId, List.of(milk()));
    }

    private static Order orderAt(String orderNumber, UUID customerId, Instant createdAt) {
        return orderAt(orderNumber, customerId, createdAt, PAYMENT_ID, List.of(milk()));
    }

    private static Order orderWithTwoItems(String orderNumber) {
        return orderAt(
                orderNumber,
                CUSTOMER_ID,
                CREATED_AT,
                PAYMENT_ID,
                List.of(
                        milk(),
                        OrderItem.create(
                                UUID.randomUUID(),
                                SECOND_PRODUCT_ID,
                                "Pan",
                                Money.cop(new BigDecimal("3.00")),
                                1)));
    }

    private static Order orderAt(
            String orderNumber, UUID customerId, Instant createdAt, UUID paymentId, List<OrderItem> items) {
        return Order.create(
                UUID.randomUUID(),
                new OrderNumber(orderNumber),
                customerId,
                items,
                new ShippingAddressSnapshot(
                        "Ada Lovelace", "Calle 1 # 2-3", "Apto 101", "Bogotá", "Cundinamarca", "3001234567"),
                paymentId,
                createdAt,
                createdAt);
    }

    private static OrderItem milk() {
        return OrderItem.create(
                UUID.randomUUID(), PRODUCT_ID, "Leche entera", Money.cop(new BigDecimal("10.50")), 2);
    }

    private void insertOrderHeader(UUID orderId, String orderNumber) {
        insertOrderHeaderWithStatus(orderId, orderNumber, "CONFIRMED");
    }

    private void insertOrderHeaderWithStatus(UUID orderId, String orderNumber, String status) {
        jdbcTemplate.update(
                connection -> {
                    PreparedStatement statement = connection.prepareStatement(
                            """
                            insert into orders.orders (
                                id, order_number, customer_id, status,
                                subtotal_amount, subtotal_currency, total_amount, total_currency,
                                shipping_recipient_name, shipping_address_line, shipping_city,
                                shipping_department, shipping_phone, created_at, updated_at
                            ) values (?, ?, ?, ?, 21.00, 'COP', 21.00, 'COP',
                                      'Ada Lovelace', 'Calle 1', 'Bogotá', 'Cundinamarca', '3001234567', ?, ?)
                            """);
                    statement.setObject(1, orderId);
                    statement.setString(2, orderNumber);
                    statement.setObject(3, CUSTOMER_ID);
                    statement.setString(4, status);
                    statement.setTimestamp(5, Timestamp.from(CREATED_AT));
                    statement.setTimestamp(6, Timestamp.from(CREATED_AT));
                    return statement;
                });
    }

    private void insertItemBypassingDomain(UUID orderId, int quantity, BigDecimal unitPrice, String currency) {
        jdbcTemplate.update(
                connection -> {
                    PreparedStatement statement = connection.prepareStatement(
                            """
                            insert into orders.order_items (
                                id, order_id, item_index, product_id, product_name,
                                unit_price_amount, unit_price_currency, quantity,
                                subtotal_amount, subtotal_currency
                            ) values (?, ?, 0, ?, 'Bypass', ?, ?, ?, ?, ?)
                            """);
                    statement.setObject(1, UUID.randomUUID());
                    statement.setObject(2, orderId);
                    statement.setObject(3, PRODUCT_ID);
                    statement.setBigDecimal(4, unitPrice);
                    statement.setString(5, currency);
                    statement.setInt(6, quantity);
                    statement.setBigDecimal(7, unitPrice);
                    statement.setString(8, currency);
                    return statement;
                });
    }
}
