package com.superfercho.shopping.application.port.in;

import com.superfercho.shopping.application.dto.favorite.RemoveFavoriteCommand;

public interface RemoveFavoriteUseCase {

    void execute(RemoveFavoriteCommand command);
}
