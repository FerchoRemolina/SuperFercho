package com.superfercho.shopping.application.service;

import com.superfercho.shopping.application.dto.favorite.AddFavoriteCommand;
import com.superfercho.shopping.application.dto.favorite.AddFavoriteResult;
import com.superfercho.shopping.application.dto.favorite.FavoriteItemResponse;
import com.superfercho.shopping.application.dto.favorite.FavoriteListResult;
import com.superfercho.shopping.application.dto.favorite.RemoveFavoriteCommand;
import com.superfercho.shopping.application.exception.DuplicateFavoriteException;
import com.superfercho.shopping.application.exception.ProductNotFoundException;
import com.superfercho.shopping.application.port.CurrentUserProvider;
import com.superfercho.shopping.application.port.in.AddFavoriteUseCase;
import com.superfercho.shopping.application.port.in.ListFavoritesUseCase;
import com.superfercho.shopping.application.port.in.RemoveFavoriteUseCase;
import com.superfercho.shopping.application.port.out.ClockPort;
import com.superfercho.shopping.application.port.out.FavoriteRepositoryPort;
import com.superfercho.shopping.application.port.out.ProductCardCatalogPort;
import com.superfercho.shopping.application.port.out.ProductCatalogPort;
import com.superfercho.shopping.domain.model.Favorite;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class FavoriteApplicationService
        implements AddFavoriteUseCase, RemoveFavoriteUseCase, ListFavoritesUseCase {

    private final CurrentUserProvider currentUserProvider;
    private final FavoriteRepositoryPort favoriteRepository;
    private final ProductCatalogPort productCatalogPort;
    private final ProductCardCatalogPort productCardCatalogPort;
    private final ClockPort clockPort;

    public FavoriteApplicationService(
            CurrentUserProvider currentUserProvider,
            FavoriteRepositoryPort favoriteRepository,
            ProductCatalogPort productCatalogPort,
            ProductCardCatalogPort productCardCatalogPort,
            ClockPort clockPort) {
        this.currentUserProvider = currentUserProvider;
        this.favoriteRepository = favoriteRepository;
        this.productCatalogPort = productCatalogPort;
        this.productCardCatalogPort = productCardCatalogPort;
        this.clockPort = clockPort;
    }

    @Override
    public AddFavoriteResult execute(AddFavoriteCommand command) {
        UUID customerId = currentUserProvider.getCurrentUserId();
        productCatalogPort
                .getProduct(command.productId())
                .orElseThrow(() -> new ProductNotFoundException(command.productId()));
        Optional<Favorite> existing =
                favoriteRepository.findByCustomerIdAndProductId(customerId, command.productId());
        if (existing.isPresent()) {
            return AddFavoriteResult.existing(existing.get());
        }
        Favorite favorite =
                Favorite.create(UUID.randomUUID(), customerId, command.productId(), clockPort.currentTime());
        try {
            return AddFavoriteResult.created(favoriteRepository.save(favorite));
        } catch (DuplicateFavoriteException exception) {
            return favoriteRepository
                    .findByCustomerIdAndProductId(customerId, command.productId())
                    .map(AddFavoriteResult::existing)
                    .orElseThrow(() -> exception);
        }
    }

    @Override
    public void execute(RemoveFavoriteCommand command) {
        UUID customerId = currentUserProvider.getCurrentUserId();
        favoriteRepository.deleteByCustomerIdAndProductId(customerId, command.productId());
    }

    @Override
    public FavoriteListResult execute() {
        UUID customerId = currentUserProvider.getCurrentUserId();
        List<Favorite> favorites = favoriteRepository.findAllByCustomerId(customerId);
        if (favorites.isEmpty()) {
            return new FavoriteListResult(List.of());
        }
        Map<UUID, Favorite> favoritesByProductId = new LinkedHashMap<>();
        for (Favorite favorite : favorites) {
            favoritesByProductId.putIfAbsent(favorite.productId(), favorite);
        }
        List<UUID> productIds = favoritesByProductId.keySet().stream().toList();
        List<FavoriteItemResponse> items = productCardCatalogPort.findCardsByIds(productIds).stream()
                .map(card -> {
                    Favorite favorite = favoritesByProductId.get(card.id());
                    if (favorite == null) {
                        return null;
                    }
                    return new FavoriteItemResponse(favorite.productId(), favorite.createdAt(), card);
                })
                .filter(item -> item != null)
                .toList();
        return new FavoriteListResult(items);
    }
}
