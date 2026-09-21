package com.superfercho.shopping.infrastructure.rest.dto;

import com.superfercho.shopping.application.dto.favorite.FavoriteItemResponse;
import java.time.Instant;
import java.util.UUID;

public record FavoriteItemRestResponse(UUID productId, Instant createdAt, FavoriteProductRestResponse product) {

    public static FavoriteItemRestResponse from(FavoriteItemResponse item) {
        return new FavoriteItemRestResponse(
                item.productId(), item.createdAt(), FavoriteProductRestResponse.from(item.product()));
    }
}
