package com.superfercho.shopping.infrastructure.rest.dto;

import com.superfercho.shopping.application.dto.favorite.FavoriteResponse;
import java.time.Instant;
import java.util.UUID;

public record FavoriteRestResponse(UUID id, UUID productId, Instant createdAt) {

    public static FavoriteRestResponse from(FavoriteResponse favorite) {
        return new FavoriteRestResponse(favorite.id(), favorite.productId(), favorite.createdAt());
    }
}
