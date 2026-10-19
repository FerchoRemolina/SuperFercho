package com.superfercho.shopping.application.service;

import com.superfercho.shopping.application.dto.shoppinglist.AddProductToShoppingListCommand;
import com.superfercho.shopping.application.dto.shoppinglist.AddProductToShoppingListResult;
import com.superfercho.shopping.application.dto.shoppinglist.ChangeShoppingListItemQuantityCommand;
import com.superfercho.shopping.application.dto.shoppinglist.ClearShoppingListCommand;
import com.superfercho.shopping.application.dto.shoppinglist.CreateShoppingListCommand;
import com.superfercho.shopping.application.dto.shoppinglist.DeleteShoppingListCommand;
import com.superfercho.shopping.application.dto.shoppinglist.GetShoppingListQuery;
import com.superfercho.shopping.application.dto.shoppinglist.RemoveProductFromShoppingListCommand;
import com.superfercho.shopping.application.dto.shoppinglist.RenameShoppingListCommand;
import com.superfercho.shopping.application.dto.shoppinglist.ShoppingListResponse;
import com.superfercho.shopping.application.exception.DuplicateShoppingListItemException;
import com.superfercho.shopping.application.exception.ProductNotFoundException;
import com.superfercho.shopping.application.exception.ShoppingListNotFoundException;
import com.superfercho.shopping.application.port.CurrentUserProvider;
import com.superfercho.shopping.application.port.in.AddProductToShoppingListUseCase;
import com.superfercho.shopping.application.port.in.ChangeShoppingListItemQuantityUseCase;
import com.superfercho.shopping.application.port.in.ClearShoppingListUseCase;
import com.superfercho.shopping.application.port.in.CreateShoppingListUseCase;
import com.superfercho.shopping.application.port.in.DeleteShoppingListUseCase;
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
                ClearShoppingListUseCase,
                DeleteShoppingListUseCase {

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
    public AddProductToShoppingListResult execute(AddProductToShoppingListCommand command) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        requireProduct(command.productId());
        Instant now = clockPort.currentTime();
        ShoppingList current = requireList(currentUserId, command.shoppingListId());
        if (containsProduct(current, command.productId())) {
            // Idempotente: el producto ya pertenece a la lista; nada cambia.
            return AddProductToShoppingListResult.alreadyPresent(current);
        }
        ShoppingList updated =
                current.addProduct(UUID.randomUUID(), command.productId(), command.quantity(), now);
        try {
            return AddProductToShoppingListResult.added(shoppingListRepository.save(updated));
        } catch (DuplicateShoppingListItemException exception) {
            // Carrera concurrente: otro request agregó el mismo producto primero.
            ShoppingList winner = requireList(currentUserId, command.shoppingListId());
            return AddProductToShoppingListResult.alreadyPresent(winner);
        }
    }

    private static boolean containsProduct(ShoppingList shoppingList, UUID productId) {
        return shoppingList.items().stream().anyMatch(item -> item.productId().equals(productId));
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

    @Override
    public void execute(DeleteShoppingListCommand command) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        ShoppingList shoppingList = requireList(currentUserId, command.shoppingListId());
        shoppingListRepository.delete(shoppingList.id());
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
