package com.superfercho.shopping.application.service;

import com.superfercho.platform.money.Money;
import com.superfercho.shopping.application.dto.ProductCatalogInfo;
import com.superfercho.shopping.application.dto.cart.AddProductToCartCommand;
import com.superfercho.shopping.application.dto.cart.CartAddOperation;
import com.superfercho.shopping.application.dto.cart.CartResponse;
import com.superfercho.shopping.application.dto.cart.ChangeCartItemQuantityCommand;
import com.superfercho.shopping.application.dto.cart.ClearCartCommand;
import com.superfercho.shopping.application.dto.cart.IdempotentAddProductToCartCommand;
import com.superfercho.shopping.application.dto.cart.IdempotentCartAddResult;
import com.superfercho.shopping.application.dto.cart.RemoveProductFromCartCommand;
import com.superfercho.shopping.application.dto.shoppinglist.AddShoppingListToCartCommand;
import com.superfercho.shopping.application.exception.CartAddOperationConflictException;
import com.superfercho.shopping.application.exception.CartNotFoundException;
import com.superfercho.shopping.application.exception.DuplicateCustomerCartException;
import com.superfercho.shopping.application.exception.ProductNotFoundException;
import com.superfercho.shopping.application.exception.ShoppingListNotFoundException;
import com.superfercho.shopping.application.port.CurrentUserProvider;
import com.superfercho.shopping.application.port.in.AddProductToCartUseCase;
import com.superfercho.shopping.application.port.in.AddShoppingListToCartUseCase;
import com.superfercho.shopping.application.port.in.ChangeCartItemQuantityUseCase;
import com.superfercho.shopping.application.port.in.ClearCartUseCase;
import com.superfercho.shopping.application.port.in.GetCartUseCase;
import com.superfercho.shopping.application.port.in.RemoveProductFromCartUseCase;
import com.superfercho.shopping.application.port.out.CartAddOperationPort;
import com.superfercho.shopping.application.port.out.CartRepositoryPort;
import com.superfercho.shopping.application.port.out.ClockPort;
import com.superfercho.shopping.application.port.out.ProductCatalogPort;
import com.superfercho.shopping.application.port.out.ShoppingListRepositoryPort;
import com.superfercho.shopping.domain.exception.InvalidCartItemException;
import com.superfercho.shopping.domain.model.Cart;
import com.superfercho.shopping.domain.model.CartStatus;
import com.superfercho.shopping.domain.model.ShoppingList;
import com.superfercho.shopping.domain.model.ShoppingListItem;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class CartApplicationService
        implements GetCartUseCase,
                AddProductToCartUseCase,
                AddShoppingListToCartUseCase,
                ChangeCartItemQuantityUseCase,
                RemoveProductFromCartUseCase,
                ClearCartUseCase {

    private final CurrentUserProvider currentUserProvider;
    private final CartRepositoryPort cartRepository;
    private final ShoppingListRepositoryPort shoppingListRepository;
    private final ProductCatalogPort productCatalogPort;
    private final ClockPort clockPort;
    private final CartAddOperationPort cartAddOperationPort;

    public CartApplicationService(
            CurrentUserProvider currentUserProvider,
            CartRepositoryPort cartRepository,
            ShoppingListRepositoryPort shoppingListRepository,
            ProductCatalogPort productCatalogPort,
            ClockPort clockPort,
            CartAddOperationPort cartAddOperationPort) {
        this.currentUserProvider = currentUserProvider;
        this.cartRepository = cartRepository;
        this.shoppingListRepository = shoppingListRepository;
        this.productCatalogPort = productCatalogPort;
        this.clockPort = clockPort;
        this.cartAddOperationPort = cartAddOperationPort;
    }

    @Override
    public CartResponse execute() {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        return priced(getOrCreateCart(currentUserId));
    }

    @Override
    public CartResponse execute(AddProductToCartCommand command) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        Instant now = clockPort.currentTime();
        Money price = requireProduct(command.productId()).currentPrice();
        return addProducts(
                currentUserId, now, List.of(new ProductAddition(command.productId(), command.quantity(), price)));
    }

    public IdempotentCartAddResult execute(IdempotentAddProductToCartCommand command) {
        UUID customerId = currentUserProvider.getCurrentUserId();
        requireIdempotentCommand(command);
        Instant now = clockPort.currentTime();
        Optional<CartAddOperation> existing = cartAddOperationPort.findAndLock(command.operationId());
        if (existing.isPresent()) {
            return replayOrConflict(existing.get(), customerId, command);
        }
        Money price = requireProduct(command.productId()).currentPrice();
        CartResponse cart = addProducts(
                customerId, now, List.of(new ProductAddition(command.productId(), command.quantity(), price)));
        cartAddOperationPort.save(new CartAddOperation(
                command.operationId(), customerId, command.productId(), command.quantity(), now));
        return IdempotentCartAddResult.applied(cart);
    }

    public Optional<IdempotentCartAddResult> replayIfApplied(UUID operationId, int quantity) {
        UUID customerId = currentUserProvider.getCurrentUserId();
        if (operationId == null || quantity < 1) {
            return Optional.empty();
        }
        Optional<CartAddOperation> existing = cartAddOperationPort.find(operationId);
        if (existing.isEmpty() || !existing.get().customerId().equals(customerId)) {
            return Optional.empty();
        }
        CartAddOperation operation = existing.get();
        if (operation.quantity() != quantity) {
            throw new CartAddOperationConflictException(operationId);
        }
        return Optional.of(IdempotentCartAddResult.replayed(priced(getOrCreateCart(customerId))));
    }

    @Override
    public CartResponse execute(AddShoppingListToCartCommand command) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        ShoppingList shoppingList = requireList(currentUserId, command.shoppingListId());
        List<ProductAddition> additions = new ArrayList<>();
        for (ShoppingListItem item : shoppingList.items()) {
            // Cantidad 0: permanece guardado en la lista pero no se compra.
            if (item.quantity() <= 0) {
                continue;
            }
            Money price = requireProduct(item.productId()).currentPrice();
            additions.add(new ProductAddition(item.productId(), item.quantity(), price));
        }
        if (additions.isEmpty()) {
            return priced(getOrCreateCart(currentUserId));
        }
        return addProducts(currentUserId, clockPort.currentTime(), additions);
    }

    @Override
    public CartResponse execute(ChangeCartItemQuantityCommand command) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        Instant now = clockPort.currentTime();
        Cart updated = requireCart(currentUserId).changeQuantity(command.productId(), command.quantity(), now);
        return priced(cartRepository.save(updated));
    }

    @Override
    public CartResponse execute(RemoveProductFromCartCommand command) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        Instant now = clockPort.currentTime();
        Cart updated = requireCart(currentUserId).removeProduct(command.productId(), now);
        return priced(cartRepository.save(updated));
    }

    @Override
    public CartResponse execute(ClearCartCommand command) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        Instant now = clockPort.currentTime();
        Cart updated = requireCart(currentUserId).clear(now);
        return priced(cartRepository.save(updated));
    }

    private CartResponse addProducts(UUID customerId, Instant now, List<ProductAddition> additions) {
        Cart cart = cartRepository.findByCustomerId(customerId).orElseGet(() -> newCart(customerId, now));
        try {
            return priced(cartRepository.save(applyAdditions(cart, additions, now)));
        } catch (DuplicateCustomerCartException exception) {
            Cart winner = cartRepository.findByCustomerId(customerId).orElseThrow(() -> exception);
            return priced(cartRepository.save(applyAdditions(winner, additions, now)));
        }
    }

    private static Cart applyAdditions(Cart cart, List<ProductAddition> additions, Instant now) {
        Cart updated = cart;
        for (ProductAddition addition : additions) {
            updated = updated.addProduct(
                    UUID.randomUUID(), addition.productId(), addition.quantity(), addition.price(), now);
        }
        return updated;
    }

    private Cart getOrCreateCart(UUID customerId) {
        return cartRepository.findByCustomerId(customerId).orElseGet(() -> createCart(customerId));
    }

    private Cart createCart(UUID customerId) {
        Instant now = clockPort.currentTime();
        try {
            return cartRepository.save(newCart(customerId, now));
        } catch (DuplicateCustomerCartException exception) {
            return cartRepository.findByCustomerId(customerId).orElseThrow(() -> exception);
        }
    }

    private Cart newCart(UUID customerId, Instant now) {
        return Cart.create(UUID.randomUUID(), customerId, CartStatus.ACTIVE, List.of(), now, now);
    }

    private Cart requireCart(UUID customerId) {
        return cartRepository.findByCustomerId(customerId).orElseThrow(() -> new CartNotFoundException(customerId));
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

    private CartResponse priced(Cart cart) {
        return CartPricing.of(cart, productCatalogPort);
    }

    private ProductCatalogInfo requireProduct(UUID productId) {
        return productCatalogPort.getProduct(productId).orElseThrow(() -> new ProductNotFoundException(productId));
    }

    private static void requireIdempotentCommand(IdempotentAddProductToCartCommand command) {
        if (command == null || command.operationId() == null || command.productId() == null) {
            throw new InvalidCartItemException("idempotent cart add requires operationId and productId");
        }
        if (command.quantity() <= 0) {
            throw new InvalidCartItemException("quantity must be greater than 0");
        }
    }

    private IdempotentCartAddResult replayOrConflict(
            CartAddOperation existing, UUID customerId, IdempotentAddProductToCartCommand command) {
        if (!existing.matches(customerId, command.productId(), command.quantity())) {
            throw new CartAddOperationConflictException(command.operationId());
        }
        return IdempotentCartAddResult.replayed(priced(getOrCreateCart(customerId)));
    }

    private record ProductAddition(UUID productId, int quantity, Money price) {}
}
