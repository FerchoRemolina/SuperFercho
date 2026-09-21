package com.superfercho.shopping.application.dto.favorite;

import com.superfercho.shopping.domain.model.Favorite;

public record AddFavoriteResult(FavoriteResponse favorite, boolean created) {

    public static AddFavoriteResult created(Favorite favorite) {
        return new AddFavoriteResult(FavoriteResponse.from(favorite), true);
    }

    public static AddFavoriteResult existing(Favorite favorite) {
        return new AddFavoriteResult(FavoriteResponse.from(favorite), false);
    }
}
