package com.superfercho.shopping.application.service;

import com.superfercho.shopping.application.dto.shoppinglist.AddProductToShoppingListCommand;
import com.superfercho.shopping.application.dto.shoppinglist.ChangeShoppingListItemQuantityCommand;
import com.superfercho.shopping.application.dto.shoppinglist.ClearShoppingListCommand;
import com.superfercho.shopping.application.dto.shoppinglist.CreateShoppingListCommand;
import com.superfercho.shopping.application.dto.shoppinglist.GetShoppingListQuery;
import com.superfercho.shopping.application.dto.shoppinglist.RemoveProductFromShoppingListCommand;
import com.superfercho.shopping.application.dto.shoppinglist.RenameShoppingListCommand;
import com.superfercho.shopping.application.dto.shoppinglist.ShoppingListResponse;
import com.superfercho.shopping.application.exception.ProductNotFoundException;
import com.superfercho.shopping.application.exception.ShoppingListNotFoundException;
import com.superfercho.shopping.application.port.CurrentUserProvider;
import com.superfercho.shopping.application.port.in.AddProductToShoppingListUseCase;
import com.superfercho.shopping.application.port.in.ChangeShoppingListItemQuantityUseCase;
import com.superfercho.shopping.application.port.in.ClearShoppingListUseCase;
import com.superfercho.shopping.application.port.in.CreateShoppingListUseCase;
import com.superfercho.shopping.application.port.in.GetShoppingListUseCase;
import com.superfercho.shopping.application.port.in.ListShoppingListsUseCase;
import com.superfercho.shopping.application.port.in.RemoveProductFromShoppingListUseCase;
import com.superfercho.shopping.application.port.in.RenameShoppingListUseCase;
import com.superfercho.shopping.application.port.out.ClockPort;
import com.superfercho.shopping.application.port.out.ProductCatalogPort;
import com.superfercho.shopping.application.port.out.ShoppingListRepositoryPort;
import com.superfercho.shopping.domain.model.ShoppingList;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ShoppingListApplicationService
        implements CreateShoppingListUseCase,
                GetShoppingListUseCase,
                ListShoppingListsUseCase,
                RenameShoppingListUseCase,
                AddProductToShoppingListUseCase,
                ChangeShoppingListItemQuantityUseCase,
                RemoveProductFromShoppingListUseCase,
                ClearShoppingListUseCase {

    private final CurrentUserProvider currentUserProvider;
    private final ShoppingListRepositoryPort shoppingListRepository;
    private final ProductCatalogPort productCatalogPort;
    private final ClockPort clockPort;

    public ShoppingListApplicationService(
            CurrentUserProvider currentUserProvider,
            ShoppingListRepositoryPort shoppingListRepository,
            ProductCatalogPort productCatalogPort,
            ClockPort clockPort) {
        this.currentUserProvider = currentUserProvider;
        this.shoppingListRepository = shoppingListRepository;
        this.productCatalogPort = productCatalogPort;
        this.clockPort = clockPort;
    }

    @Override
    public ShoppingListResponse execute(CreateShoppingListCommand command) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        Instant now = clockPort.currentTime();
        ShoppingList created = ShoppingList.create(
                UUID.randomUUID(), currentUserId, command.name(), List.of(), now, now);
        return ShoppingListResponse.from(shoppingListRepository.save(created));
    }

    @Override
    public ShoppingListResponse execute(GetShoppingListQuery query) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        return ShoppingListResponse.from(requireList(currentUserId, query.shoppingListId()));
    }

    @Override
    public List<ShoppingListResponse> execute() {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        return shoppingListRepository.findAllByCustomerId(currentUserId).stream()
                .map(ShoppingListResponse::from)
                .toList();
    }

    @Override
    public ShoppingListResponse execute(RenameShoppingListCommand command) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        Instant now = clockPort.currentTime();
        ShoppingList updated =
                requireList(currentUserId, command.shoppingListId()).changeName(command.name(), now);
        return ShoppingListResponse.from(shoppingListRepository.save(updated));
    }

    @Override
    public ShoppingListResponse execute(AddProductToShoppingListCommand command) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        requireProduct(command.productId());
        Instant now = clockPort.currentTime();
        ShoppingList updated = requireList(currentUserId, command.shoppingListId())
                .addProduct(UUID.randomUUID(), command.productId(), command.quantity(), now);
        return ShoppingListResponse.from(shoppingListRepository.save(updated));
    }

    @Override
    public ShoppingListResponse execute(ChangeShoppingListItemQuantityCommand command) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        Instant now = clockPort.currentTime();
        ShoppingList updated = requireList(currentUserId, command.shoppingListId())
                .changeQuantity(command.productId(), command.quantity(), now);
        return ShoppingListResponse.from(shoppingListRepository.save(updated));
    }

    @Override
    public ShoppingListResponse execute(RemoveProductFromShoppingListCommand command) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        Instant now = clockPort.currentTime();
        ShoppingList updated = requireList(currentUserId, command.shoppingListId())
                .removeProduct(command.productId(), now);
        return ShoppingListResponse.from(shoppingListRepository.save(updated));
    }

    @Override
    public ShoppingListResponse execute(ClearShoppingListCommand command) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        Instant now = clockPort.currentTime();
        ShoppingList updated = requireList(currentUserId, command.shoppingListId()).clear(now);
        return ShoppingListResponse.from(shoppingListRepository.save(updated));
    }

    private ShoppingList requireList(UUID customerId, UUID shoppingListId) {
        ShoppingList shoppingList = shoppingListRepository
                .findById(shoppingListId)
                .orElseThrow(() -> new ShoppingListNotFoundException(shoppingListId));
        if (!shoppingList.customerId().equals(customerId)) {
            throw new ShoppingListNotFoundException(shoppingListId);
        }
        return shoppingList;
    }

    private void requireProduct(UUID productId) {
        productCatalogPort.getProduct(productId).orElseThrow(() -> new ProductNotFoundException(productId));
    }
}
