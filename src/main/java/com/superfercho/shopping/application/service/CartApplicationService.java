package com.superfercho.shopping.application.service;

import com.superfercho.platform.money.Money;
import com.superfercho.shopping.application.dto.ProductCatalogInfo;
import com.superfercho.shopping.application.dto.cart.AddProductToCartCommand;
import com.superfercho.shopping.application.dto.cart.CartResponse;
import com.superfercho.shopping.application.dto.cart.ChangeCartItemQuantityCommand;
import com.superfercho.shopping.application.dto.cart.ClearCartCommand;
import com.superfercho.shopping.application.dto.cart.RemoveProductFromCartCommand;
import com.superfercho.shopping.application.exception.CartNotFoundException;
import com.superfercho.shopping.application.exception.DuplicateCustomerCartException;
import com.superfercho.shopping.application.exception.ProductNotFoundException;
import com.superfercho.shopping.application.port.CurrentUserProvider;
import com.superfercho.shopping.application.port.in.AddProductToCartUseCase;
import com.superfercho.shopping.application.port.in.ChangeCartItemQuantityUseCase;
import com.superfercho.shopping.application.port.in.ClearCartUseCase;
import com.superfercho.shopping.application.port.in.GetCartUseCase;
import com.superfercho.shopping.application.port.in.RemoveProductFromCartUseCase;
import com.superfercho.shopping.application.port.out.CartRepositoryPort;
import com.superfercho.shopping.application.port.out.ClockPort;
import com.superfercho.shopping.application.port.out.ProductCatalogPort;
import com.superfercho.shopping.domain.model.Cart;
import com.superfercho.shopping.domain.model.CartStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class CartApplicationService
        implements GetCartUseCase,
                AddProductToCartUseCase,
                ChangeCartItemQuantityUseCase,
                RemoveProductFromCartUseCase,
                ClearCartUseCase {

    private final CurrentUserProvider currentUserProvider;
    private final CartRepositoryPort cartRepository;
    private final ProductCatalogPort productCatalogPort;
    private final ClockPort clockPort;

    public CartApplicationService(
            CurrentUserProvider currentUserProvider,
            CartRepositoryPort cartRepository,
            ProductCatalogPort productCatalogPort,
            ClockPort clockPort) {
        this.currentUserProvider = currentUserProvider;
        this.cartRepository = cartRepository;
        this.productCatalogPort = productCatalogPort;
        this.clockPort = clockPort;
    }

    @Override
    public CartResponse execute() {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        return CartResponse.from(getOrCreateCart(currentUserId));
    }

    @Override
    public CartResponse execute(AddProductToCartCommand command) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        Instant now = clockPort.currentTime();
        Money price = requireProduct(command.productId()).currentPrice();
        Cart cart = cartRepository
                .findByCustomerId(currentUserId)
                .orElseGet(() -> newCart(currentUserId, now));
        Cart updated =
                cart.addProduct(UUID.randomUUID(), command.productId(), command.quantity(), price, now);
        try {
            return CartResponse.from(cartRepository.save(updated));
        } catch (DuplicateCustomerCartException exception) {
            Cart winner = cartRepository
                    .findByCustomerId(currentUserId)
                    .orElseThrow(() -> exception);
            Cart retried =
                    winner.addProduct(UUID.randomUUID(), command.productId(), command.quantity(), price, now);
            return CartResponse.from(cartRepository.save(retried));
        }
    }

    @Override
    public CartResponse execute(ChangeCartItemQuantityCommand command) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        Instant now = clockPort.currentTime();
        Cart updated = requireCart(currentUserId).changeQuantity(command.productId(), command.quantity(), now);
        return CartResponse.from(cartRepository.save(updated));
    }

    @Override
    public CartResponse execute(RemoveProductFromCartCommand command) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        Instant now = clockPort.currentTime();
        Cart updated = requireCart(currentUserId).removeProduct(command.productId(), now);
        return CartResponse.from(cartRepository.save(updated));
    }

    @Override
    public CartResponse execute(ClearCartCommand command) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        Instant now = clockPort.currentTime();
        Cart updated = requireCart(currentUserId).clear(now);
        return CartResponse.from(cartRepository.save(updated));
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

    private ProductCatalogInfo requireProduct(UUID productId) {
        return productCatalogPort.getProduct(productId).orElseThrow(() -> new ProductNotFoundException(productId));
    }
}
