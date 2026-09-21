package com.superfercho.shopping.infrastructure.rest.dto;

import com.superfercho.shopping.application.dto.favorite.FavoriteListResult;
import java.util.List;

public record FavoriteListRestResponse(List<FavoriteItemRestResponse> items) {

    public static FavoriteListRestResponse from(FavoriteListResult result) {
        return new FavoriteListRestResponse(
                result.items().stream().map(FavoriteItemRestResponse::from).toList());
    }
}
