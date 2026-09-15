package com.superfercho.catalog.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.superfercho.catalog.application.dto.StockDecrementResult;
import com.superfercho.catalog.application.dto.StockQuantity;
import com.superfercho.catalog.application.exception.DuplicateBarcodeException;
import com.superfercho.catalog.application.exception.InvalidCategoryReferenceException;
import com.superfercho.catalog.application.port.CategoryRepository;
import com.superfercho.catalog.application.port.InventoryPort;
import com.superfercho.catalog.application.port.ProductRepository;
import com.superfercho.catalog.domain.model.Category;
import com.superfercho.catalog.domain.model.CategoryStatus;
import com.superfercho.catalog.domain.model.Product;
import com.superfercho.catalog.domain.model.ProductStatus;
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
class CatalogPersistenceAdapterTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

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
        registry.add(
                "superfercho.security.jwt.secret", () -> "test-only-superfercho-jwt-secret-key-32b");
    }

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryPort inventoryPort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldPersistAndReloadCategory() {
        Category saved = categoryRepository.save(newCategory("Frutas", CategoryStatus.ACTIVE));

        Category loaded = categoryRepository.findById(saved.id()).orElseThrow();

        assertThat(loaded.name()).isEqualTo("Frutas");
        assertThat(loaded.description()).isEqualTo("Fresh produce");
        assertThat(loaded.status()).isEqualTo(CategoryStatus.ACTIVE);
        assertThat(loaded.createdAt()).isEqualTo(NOW);
        assertThat(loaded.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void shouldPersistCategoryStatus() {
        Category saved = categoryRepository.save(newCategory("Inactiva", CategoryStatus.INACTIVE));

        assertThat(categoryRepository.findById(saved.id()).orElseThrow().status())
                .isEqualTo(CategoryStatus.INACTIVE);
    }

    @Test
    void shouldPersistAndReloadProduct() {
        Category category = categoryRepository.save(newCategory("Lácteos", CategoryStatus.ACTIVE));
        Product saved = productRepository.save(
                newProduct(category.id(), "770111", "Leche entera", ProductStatus.ACTIVE, 10, "4500.50"));

        Product loaded = productRepository.findById(saved.id()).orElseThrow();

        assertThat(loaded.categoryId()).isEqualTo(category.id());
        assertThat(loaded.barcode()).isEqualTo("770111");
        assertThat(loaded.name()).isEqualTo("Leche entera");
        assertThat(loaded.brand()).isEqualTo("Alpina");
        assertThat(loaded.price()).isEqualTo(Money.cop(new BigDecimal("4500.50")));
        assertThat(loaded.stock()).isEqualTo(10);
        assertThat(loaded.status()).isEqualTo(ProductStatus.ACTIVE);
    }

    @Test
    void shouldPersistProductStatusPriceAndStock() {
        Category category = categoryRepository.save(newCategory("Abarrotes", CategoryStatus.ACTIVE));
        Product saved = productRepository.save(
                newProduct(category.id(), null, "Arroz", ProductStatus.INACTIVE, 0, "1200.00"));

        Product loaded = productRepository.findById(saved.id()).orElseThrow();

        assertThat(loaded.status()).isEqualTo(ProductStatus.INACTIVE);
        assertThat(loaded.price().amount()).isEqualByComparingTo("1200.00");
        assertThat(loaded.stock()).isEqualTo(0);
        assertThat(loaded.barcode()).isNull();
    }

    @Test
    void shouldRejectDuplicateBarcode() {
        Category category = categoryRepository.save(newCategory("Bebidas", CategoryStatus.ACTIVE));
        productRepository.save(newProduct(category.id(), "770999", "Agua", ProductStatus.ACTIVE, 5, "1000.00"));

        assertThatThrownBy(
                        () -> productRepository.save(
                                newProduct(category.id(), "770999", "Agua gas", ProductStatus.ACTIVE, 3, "1200.00")))
                .isInstanceOf(DuplicateBarcodeException.class);
    }

    @Test
    void shouldAllowMultipleProductsWithoutBarcode() {
        Category category = categoryRepository.save(newCategory("Panadería", CategoryStatus.ACTIVE));

        Product first = productRepository.save(newProduct(category.id(), null, "Pan", ProductStatus.ACTIVE, 2, "500.00"));
        Product second =
                productRepository.save(newProduct(category.id(), null, "Croissant", ProductStatus.ACTIVE, 4, "800.00"));

        assertThat(productRepository.findById(first.id())).isPresent();
        assertThat(productRepository.findById(second.id())).isPresent();
    }

    @Test
    void shouldRejectProductWhenCategoryDoesNotExist() {
        UUID missingCategoryId = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");

        assertThatThrownBy(
                        () -> productRepository.save(
                                newProduct(missingCategoryId, "770000", "Huérfano", ProductStatus.ACTIVE, 1, "100.00")))
                .isInstanceOf(InvalidCategoryReferenceException.class);
    }

    @Test
    void shouldRejectNegativePriceAtDatabase() {
        Category category = categoryRepository.save(newCategory("Precio", CategoryStatus.ACTIVE));

        assertThatThrownBy(() -> insertProductBypassingDomain(category.id(), new BigDecimal("-0.01"), 1))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectNegativeStockAtDatabase() {
        Category category = categoryRepository.save(newCategory("Stock", CategoryStatus.ACTIVE));

        assertThatThrownBy(() -> insertProductBypassingDomain(category.id(), new BigDecimal("1.00"), -1))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldListAndFilterProducts() {
        Category dairy = categoryRepository.save(newCategory("Filtrar-lacteos", CategoryStatus.ACTIVE));
        Category fruit = categoryRepository.save(newCategory("Filtrar-frutas", CategoryStatus.ACTIVE));
        Product milk = productRepository.save(
                newProduct(dairy.id(), "770201", "Leche", ProductStatus.ACTIVE, 8, "2000.00"));
        Product inactiveMilk = productRepository.save(
                newProduct(dairy.id(), "770202", "Leche inactiva", ProductStatus.INACTIVE, 1, "2000.00"));
        productRepository.save(newProduct(fruit.id(), "770203", "Manzana", ProductStatus.ACTIVE, 6, "300.00"));

        assertThat(productRepository.findByCategoryId(dairy.id()))
                .extracting(Product::id)
                .containsExactlyInAnyOrder(milk.id(), inactiveMilk.id());
        assertThat(productRepository.findByStatus(ProductStatus.INACTIVE))
                .extracting(Product::id)
                .contains(inactiveMilk.id());
        assertThat(productRepository.findByCategoryIdAndStatus(dairy.id(), ProductStatus.ACTIVE))
                .extracting(Product::id)
                .containsExactly(milk.id());
        assertThat(categoryRepository.findByStatus(CategoryStatus.ACTIVE))
                .extracting(Category::id)
                .contains(dairy.id(), fruit.id());
    }

    @Test
    void shouldSearchProductsByNameBrandOrBarcode() {
        Category category = categoryRepository.save(newCategory("Buscar", CategoryStatus.ACTIVE));
        Product milk = productRepository.save(
                newProduct(category.id(), "770888111", "Zarzamora especial", ProductStatus.ACTIVE, 3, "2500.00"));
        productRepository.save(newProduct(category.id(), "770888222", "Queso fresco", ProductStatus.ACTIVE, 2, "8000.00"));

        assertThat(productRepository.searchByNameBrandOrBarcode("zarzamora"))
                .extracting(Product::id)
                .containsExactly(milk.id());
        assertThat(productRepository.searchByNameBrandOrBarcode("alpina"))
                .extracting(Product::id)
                .contains(milk.id());
        assertThat(productRepository.searchByNameBrandOrBarcode("770888111"))
                .extracting(Product::id)
                .containsExactly(milk.id());
    }

    @Test
    void shouldDecrementAndRestoreStockAtomically() {
        Category category = categoryRepository.save(newCategory("Inventario", CategoryStatus.ACTIVE));
        Product available = productRepository.save(
                newProduct(category.id(), "770301", "Disponible", ProductStatus.ACTIVE, 2, "1000.00"));
        Product empty = productRepository.save(
                newProduct(category.id(), "770302", "Agotado", ProductStatus.ACTIVE, 0, "1000.00"));

        StockDecrementResult failed = inventoryPort.decreaseStockAtomically(
                List.of(new StockQuantity(available.id(), 1), new StockQuantity(empty.id(), 1)));

        assertThat(failed.succeeded()).isFalse();
        assertThat(failed.unavailableProductIds()).containsExactly(empty.id());
        assertThat(productRepository.findById(available.id()).orElseThrow().stock()).isEqualTo(2);

        StockDecrementResult success =
                inventoryPort.decreaseStockAtomically(List.of(new StockQuantity(available.id(), 2)));

        assertThat(success.succeeded()).isTrue();
        assertThat(productRepository.findById(available.id()).orElseThrow().stock()).isEqualTo(0);

        inventoryPort.restoreStock(List.of(new StockQuantity(available.id(), 2)));
        assertThat(productRepository.findById(available.id()).orElseThrow().stock()).isEqualTo(2);
    }

    private void insertProductBypassingDomain(UUID categoryId, BigDecimal price, int stock) {
        jdbcTemplate.update(
                connection -> {
                    PreparedStatement statement = connection.prepareStatement(
                            """
                            insert into catalog.products (
                                id, category_id, name, price_amount, currency, stock, status, created_at, updated_at
                            ) values (?, ?, 'Bypass', ?, 'COP', ?, 'ACTIVE', ?, ?)
                            """);
                    statement.setObject(1, UUID.randomUUID());
                    statement.setObject(2, categoryId);
                    statement.setBigDecimal(3, price);
                    statement.setInt(4, stock);
                    statement.setTimestamp(5, Timestamp.from(NOW));
                    statement.setTimestamp(6, Timestamp.from(NOW));
                    return statement;
                });
    }

    private static Category newCategory(String name, CategoryStatus status) {
        return Category.create(UUID.randomUUID(), name, "Fresh produce", status, NOW, NOW);
    }

    private static Product newProduct(
            UUID categoryId, String barcode, String name, ProductStatus status, int stock, String price) {
        return Product.create(
                UUID.randomUUID(),
                categoryId,
                barcode,
                name,
                "Alpina",
                "1L",
                Money.cop(new BigDecimal(price)),
                stock,
                null,
                status,
                NOW,
                NOW);
    }
}
