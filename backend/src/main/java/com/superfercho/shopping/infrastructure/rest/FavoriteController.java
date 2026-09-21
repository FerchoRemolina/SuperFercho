package com.superfercho.shopping.infrastructure.rest;

import com.superfercho.shopping.application.dto.favorite.AddFavoriteCommand;
import com.superfercho.shopping.application.dto.favorite.AddFavoriteResult;
import com.superfercho.shopping.application.dto.favorite.RemoveFavoriteCommand;
import com.superfercho.shopping.application.port.in.AddFavoriteUseCase;
import com.superfercho.shopping.application.port.in.ListFavoritesUseCase;
import com.superfercho.shopping.application.port.in.RemoveFavoriteUseCase;
import com.superfercho.shopping.infrastructure.rest.dto.FavoriteListRestResponse;
import com.superfercho.shopping.infrastructure.rest.dto.FavoriteRestResponse;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@Profile("!test")
@RequestMapping("/api/v1/favorites")
public class FavoriteController {

    private final ListFavoritesUseCase listFavoritesUseCase;
    private final AddFavoriteUseCase addFavoriteUseCase;
    private final RemoveFavoriteUseCase removeFavoriteUseCase;

    public FavoriteController(
            ListFavoritesUseCase listFavoritesUseCase,
            AddFavoriteUseCase addFavoriteUseCase,
            RemoveFavoriteUseCase removeFavoriteUseCase) {
        this.listFavoritesUseCase = listFavoritesUseCase;
        this.addFavoriteUseCase = addFavoriteUseCase;
        this.removeFavoriteUseCase = removeFavoriteUseCase;
    }

    @GetMapping
    public FavoriteListRestResponse list() {
        return FavoriteListRestResponse.from(listFavoritesUseCase.execute());
    }

    @PostMapping("/{productId}")
    public ResponseEntity<FavoriteRestResponse> add(@PathVariable UUID productId) {
        AddFavoriteResult result = addFavoriteUseCase.execute(new AddFavoriteCommand(productId));
        FavoriteRestResponse body = FavoriteRestResponse.from(result.favorite());
        if (result.created()) {
            return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().build().toUri())
                    .body(body);
        }
        return ResponseEntity.ok(body);
    }

    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable UUID productId) {
        removeFavoriteUseCase.execute(new RemoveFavoriteCommand(productId));
    }
}
