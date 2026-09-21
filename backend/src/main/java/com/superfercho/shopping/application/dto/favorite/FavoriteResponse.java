package com.superfercho.shopping.application.dto.favorite;

import com.superfercho.shopping.domain.model.Favorite;
import java.time.Instant;
import java.util.UUID;

public record FavoriteResponse(UUID id, UUID productId, Instant createdAt) {

    public static FavoriteResponse from(Favorite favorite) {
        return new FavoriteResponse(favorite.id(), favorite.productId(), favorite.createdAt());
    }
}
