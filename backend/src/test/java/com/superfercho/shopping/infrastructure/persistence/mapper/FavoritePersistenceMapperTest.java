package com.superfercho.shopping.infrastructure.persistence.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.superfercho.shopping.domain.model.Favorite;
import com.superfercho.shopping.infrastructure.persistence.entity.FavoriteJpaEntity;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FavoritePersistenceMapperTest {

    private static final UUID ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID CUSTOMER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID PRODUCT_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final Instant CREATED_AT = Instant.parse("2026-09-21T10:00:00Z");

    private final FavoritePersistenceMapper mapper = new FavoritePersistenceMapper();

    @Test
    void shouldMapFavoriteRoundTrip() {
        Favorite favorite = Favorite.create(ID, CUSTOMER_ID, PRODUCT_ID, CREATED_AT);

        FavoriteJpaEntity entity = mapper.toEntity(favorite);
        Favorite mapped = mapper.toDomain(entity);

        assertEquals(ID, entity.getId());
        assertEquals(CUSTOMER_ID, entity.getCustomerId());
        assertEquals(PRODUCT_ID, entity.getProductId());
        assertEquals(CREATED_AT, entity.getCreatedAt());
        assertEquals(favorite, mapped);
    }
}
