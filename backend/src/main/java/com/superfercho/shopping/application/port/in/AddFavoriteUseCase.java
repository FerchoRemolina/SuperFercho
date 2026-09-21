package com.superfercho.shopping.application.port.in;

import com.superfercho.shopping.application.dto.favorite.AddFavoriteCommand;
import com.superfercho.shopping.application.dto.favorite.AddFavoriteResult;

public interface AddFavoriteUseCase {

    AddFavoriteResult execute(AddFavoriteCommand command);
}
