package com.superfercho.shopping.infrastructure.rest.dto;

import com.superfercho.platform.money.Money;
import com.superfercho.shopping.application.dto.favorite.FavoriteProductView;
import java.util.UUID;

public record FavoriteProductRestResponse(
        UUID id,
        String name,
        String brand,
        Money price,
        String imageUrl,
        UUID categoryId,
        String status,
        boolean available) {

    public static FavoriteProductRestResponse from(FavoriteProductView product) {
        if (product == null) {
            return null;
        }
        return new FavoriteProductRestResponse(
                product.id(),
                product.name(),
                product.brand(),
                product.price(),
                product.imageUrl(),
                product.categoryId(),
                product.status(),
                product.available());
    }
}
