package com.superfercho.shopping.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.superfercho.shopping.application.port.out.ShoppingListRepositoryPort;
import com.superfercho.shopping.domain.model.ShoppingList;
import com.superfercho.shopping.domain.model.ShoppingListItem;
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
class ShoppingListPersistenceAdapterTest {

    private static final Instant CREATED_AT = Instant.parse("2026-03-01T10:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-03-01T10:20:00Z");
    private static final Instant ITEM_CREATED_AT = Instant.parse("2026-03-01T10:05:00Z");
    private static final Instant SECOND_ITEM_CREATED_AT = Instant.parse("2026-03-01T10:10:00Z");
    private static final UUID PRODUCT_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID SECOND_PRODUCT_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

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
    private ShoppingListRepositoryPort shoppingListRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldPersistAndReloadShoppingListById() {
        UUID customerId = UUID.randomUUID();
        ShoppingList saved = shoppingListRepository.save(list(customerId, "Weekly groceries", List.of(milk())));

        ShoppingList loaded = shoppingListRepository.findById(saved.id()).orElseThrow();

        assertThat(loaded.id()).isEqualTo(saved.id());
        assertThat(loaded.customerId()).isEqualTo(customerId);
        assertThat(loaded.name()).isEqualTo("Weekly groceries");
        assertThat(loaded.createdAt()).isEqualTo(CREATED_AT);
        assertThat(loaded.updatedAt()).isEqualTo(UPDATED_AT);
        assertThat(loaded.items()).hasSize(1);
        assertThat(loaded.items().get(0).productId()).isEqualTo(PRODUCT_ID);
        assertThat(loaded.items().get(0).quantity()).isEqualTo(3);
        assertThat(loaded.items().get(0).createdAt()).isEqualTo(ITEM_CREATED_AT);
    }

    @Test
    void shouldPersistEmptyShoppingList() {
        UUID customerId = UUID.randomUUID();
        ShoppingList saved = shoppingListRepository.save(list(customerId, "Empty list", List.of()));

        ShoppingList loaded = shoppingListRepository.findById(saved.id()).orElseThrow();

        assertThat(loaded.id()).isEqualTo(saved.id());
        assertThat(loaded.name()).isEqualTo("Empty list");
        assertThat(loaded.items()).isEmpty();
    }

    @Test
    void shouldPersistMultipleItemsAndPreserveOrder() {
        UUID customerId = UUID.randomUUID();
        ShoppingList saved =
                shoppingListRepository.save(list(customerId, "Weekly groceries", List.of(milk(), bread())));

        ShoppingList loaded = shoppingListRepository.findById(saved.id()).orElseThrow();

        assertThat(loaded.items()).hasSize(2);
        assertThat(loaded.items().get(0).productId()).isEqualTo(PRODUCT_ID);
        assertThat(loaded.items().get(1).productId()).isEqualTo(SECOND_PRODUCT_ID);
        assertThat(loaded.items().get(0).quantity()).isEqualTo(3);
        assertThat(loaded.items().get(1).quantity()).isEqualTo(1);
        assertThat(loaded.items().get(0).createdAt()).isEqualTo(ITEM_CREATED_AT);
        assertThat(loaded.items().get(1).createdAt()).isEqualTo(SECOND_ITEM_CREATED_AT);
    }

    @Test
    void shouldFindShoppingListsByCustomerIdOrderedByNameIgnoreCase() {
        UUID customerId = UUID.randomUUID();
        UUID otherCustomerId = UUID.randomUUID();
        ShoppingList zeta = shoppingListRepository.save(list(customerId, "zeta", List.of(milk())));
        ShoppingList alpha = shoppingListRepository.save(list(customerId, "Alpha", List.of(bread())));
        shoppingListRepository.save(list(otherCustomerId, "Other", List.of(bread())));

        assertThat(shoppingListRepository.findAllByCustomerId(customerId))
                .extracting(ShoppingList::id)
                .containsExactly(alpha.id(), zeta.id());
        assertThat(shoppingListRepository.findAllByCustomerId(UUID.randomUUID())).isEmpty();
    }

    @Test
    void shouldDeleteShoppingListAndItemsWithoutRemovingCatalogProduct() {
        UUID catalogProductId = insertCatalogProduct("Leche delete");
        UUID customerId = UUID.randomUUID();
        ShoppingList saved = shoppingListRepository.save(list(
                customerId,
                "To delete",
                List.of(ShoppingListItem.create(UUID.randomUUID(), catalogProductId, 2, ITEM_CREATED_AT))));

        shoppingListRepository.delete(saved.id());

        assertThat(shoppingListRepository.findById(saved.id())).isEmpty();
        Integer remainingItems = jdbcTemplate.queryForObject(
                "select count(*) from shopping.shopping_list_items where shopping_list_id = ?",
                Integer.class,
                saved.id());
        assertThat(remainingItems).isZero();
        Integer remainingProducts = jdbcTemplate.queryForObject(
                "select count(*) from catalog.products where id = ?", Integer.class, catalogProductId);
        assertThat(remainingProducts).isEqualTo(1);
    }

    @Test
    void shouldRejectInvalidQuantityAtDatabase() {
        UUID listId = UUID.randomUUID();
        insertListHeader(listId, UUID.randomUUID());

        assertThatThrownBy(() -> insertItemBypassingDomain(listId, 0))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectItemWhenShoppingListDoesNotExist() {
        assertThatThrownBy(() -> insertItemBypassingDomain(UUID.randomUUID(), 1))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private static ShoppingList list(UUID customerId, String name, List<ShoppingListItem> items) {
        return ShoppingList.reconstitute(UUID.randomUUID(), customerId, name, items, CREATED_AT, UPDATED_AT);
    }

    private static ShoppingListItem milk() {
        return ShoppingListItem.create(UUID.randomUUID(), PRODUCT_ID, 3, ITEM_CREATED_AT);
    }

    private static ShoppingListItem bread() {
        return ShoppingListItem.create(UUID.randomUUID(), SECOND_PRODUCT_ID, 1, SECOND_ITEM_CREATED_AT);
    }

    private void insertListHeader(UUID listId, UUID customerId) {
        jdbcTemplate.update(
                connection -> {
                    PreparedStatement statement = connection.prepareStatement(
                            """
                            insert into shopping.shopping_lists (
                                id, customer_id, name, created_at, updated_at
                            ) values (?, ?, 'Bypass', ?, ?)
                            """);
                    statement.setObject(1, listId);
                    statement.setObject(2, customerId);
                    statement.setTimestamp(3, Timestamp.from(CREATED_AT));
                    statement.setTimestamp(4, Timestamp.from(CREATED_AT));
                    return statement;
                });
    }

    private void insertItemBypassingDomain(UUID listId, int quantity) {
        jdbcTemplate.update(
                connection -> {
                    PreparedStatement statement = connection.prepareStatement(
                            """
                            insert into shopping.shopping_list_items (
                                id, shopping_list_id, item_index, product_id, quantity, created_at
                            ) values (?, ?, 0, ?, ?, ?)
                            """);
                    statement.setObject(1, UUID.randomUUID());
                    statement.setObject(2, listId);
                    statement.setObject(3, PRODUCT_ID);
                    statement.setInt(4, quantity);
                    statement.setTimestamp(5, Timestamp.from(ITEM_CREATED_AT));
                    return statement;
                });
    }

    private UUID insertCatalogProduct(String name) {
        UUID categoryId = UUID.randomUUID();
        UUID productTypeId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Instant now = CREATED_AT;
        jdbcTemplate.update(
                """
                insert into catalog.categories (id, name, description, status, created_at, updated_at)
                values (?, ?, null, 'ACTIVE', ?, ?)
                """,
                categoryId,
                "Cat-" + categoryId.toString().substring(0, 8),
                Timestamp.from(now),
                Timestamp.from(now));
        jdbcTemplate.update(
                """
                insert into catalog.product_types (
                    id, category_id, name, description, status, created_at, updated_at
                ) values (?, ?, ?, null, 'ACTIVE', ?, ?)
                """,
                productTypeId,
                categoryId,
                "Type-" + productTypeId.toString().substring(0, 8),
                Timestamp.from(now),
                Timestamp.from(now));
        jdbcTemplate.update(
                """
                insert into catalog.products (
                    id, category_id, product_type_id, presentation_quantity, presentation_unit,
                    name, price_amount, currency, stock, status, created_at, updated_at
                ) values (?, ?, ?, 1, 'UNIT', ?, 1000.00, 'COP', 5, 'ACTIVE', ?, ?)
                """,
                productId,
                categoryId,
                productTypeId,
                name,
                Timestamp.from(now),
                Timestamp.from(now));
        return productId;
    }
}
