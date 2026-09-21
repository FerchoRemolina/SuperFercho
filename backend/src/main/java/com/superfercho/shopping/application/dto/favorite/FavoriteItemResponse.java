package com.superfercho.shopping.application.dto.favorite;

import java.time.Instant;
import java.util.UUID;

public record FavoriteItemResponse(UUID productId, Instant createdAt, FavoriteProductView product) {}
