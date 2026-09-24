package com.superfercho.shopping.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.superfercho.shopping.application.exception.DuplicateFavoriteException;
import com.superfercho.shopping.application.port.out.FavoriteRepositoryPort;
import com.superfercho.shopping.domain.model.Favorite;
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
class FavoritePersistenceAdapterTest {

    private static final Instant CREATED_AT = Instant.parse("2026-09-21T10:00:00Z");
    private static final Instant LATER = Instant.parse("2026-09-21T11:00:00Z");
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
    private FavoriteRepositoryPort favoriteRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldPersistAndReloadFavorite() {
        UUID customerId = UUID.randomUUID();
        Favorite saved = favoriteRepository.save(favorite(customerId, PRODUCT_ID, CREATED_AT));

        Favorite loaded = favoriteRepository
                .findByCustomerIdAndProductId(customerId, PRODUCT_ID)
                .orElseThrow();

        assertThat(loaded.id()).isEqualTo(saved.id());
        assertThat(loaded.customerId()).isEqualTo(customerId);
        assertThat(loaded.productId()).isEqualTo(PRODUCT_ID);
        assertThat(loaded.createdAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void shouldListFavoritesForCustomer() {
        UUID customerId = UUID.randomUUID();
        favoriteRepository.save(favorite(customerId, PRODUCT_ID, CREATED_AT));
        favoriteRepository.save(favorite(customerId, SECOND_PRODUCT_ID, LATER));

        List<Favorite> favorites = favoriteRepository.findAllByCustomerId(customerId);

        assertThat(favorites).hasSize(2);
        assertThat(favorites)
                .extracting(Favorite::productId)
                .containsExactlyInAnyOrder(PRODUCT_ID, SECOND_PRODUCT_ID);
    }

    @Test
    void shouldRejectDuplicateCustomerProductAtDatabase() {
        UUID customerId = UUID.randomUUID();
        favoriteRepository.save(favorite(customerId, PRODUCT_ID, CREATED_AT));

        assertThatThrownBy(() -> favoriteRepository.save(favorite(customerId, PRODUCT_ID, LATER)))
                .isInstanceOf(DuplicateFavoriteException.class);
    }

    @Test
    void shouldAllowSameProductForDifferentCustomers() {
        UUID customerId = UUID.randomUUID();
        UUID otherCustomerId = UUID.randomUUID();

        favoriteRepository.save(favorite(customerId, PRODUCT_ID, CREATED_AT));
        favoriteRepository.save(favorite(otherCustomerId, PRODUCT_ID, CREATED_AT));

        assertThat(favoriteRepository.findByCustomerIdAndProductId(customerId, PRODUCT_ID)).isPresent();
        assertThat(favoriteRepository.findByCustomerIdAndProductId(otherCustomerId, PRODUCT_ID)).isPresent();
    }

    @Test
    void shouldDeleteExistingFavoriteAndNoOpWhenMissing() {
        UUID customerId = UUID.randomUUID();
        favoriteRepository.save(favorite(customerId, PRODUCT_ID, CREATED_AT));

        favoriteRepository.deleteByCustomerIdAndProductId(customerId, PRODUCT_ID);
        favoriteRepository.deleteByCustomerIdAndProductId(customerId, PRODUCT_ID);

        assertThat(favoriteRepository.findByCustomerIdAndProductId(customerId, PRODUCT_ID)).isEmpty();
    }

    @Test
    void shouldRejectDuplicateCustomerProductWhenInsertingDirectly() {
        UUID customerId = UUID.randomUUID();
        insertFavorite(UUID.randomUUID(), customerId, PRODUCT_ID, CREATED_AT);

        assertThatThrownBy(() -> insertFavorite(UUID.randomUUID(), customerId, PRODUCT_ID, LATER))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private static Favorite favorite(UUID customerId, UUID productId, Instant createdAt) {
        return Favorite.create(UUID.randomUUID(), customerId, productId, createdAt);
    }

    private void insertFavorite(UUID id, UUID customerId, UUID productId, Instant createdAt) {
        jdbcTemplate.update(
                connection -> {
                    PreparedStatement statement = connection.prepareStatement(
                            """
                            insert into shopping.favorites (
                                id, customer_id, product_id, created_at
                            ) values (?, ?, ?, ?)
                            """);
                    statement.setObject(1, id);
                    statement.setObject(2, customerId);
                    statement.setObject(3, productId);
                    statement.setTimestamp(4, Timestamp.from(createdAt));
                    return statement;
                });
    }
}
