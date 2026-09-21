package com.superfercho.shopping.application.dto.favorite;

import com.superfercho.platform.money.Money;
import java.util.UUID;

public record FavoriteProductView(
        UUID id,
        String name,
        String brand,
        Money price,
        String imageUrl,
        UUID categoryId,
        String status,
        boolean available) {}
