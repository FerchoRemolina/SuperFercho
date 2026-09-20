package com.superfercho.shopping.domain.model;

import com.superfercho.platform.money.Money;
import com.superfercho.shopping.domain.exception.InvalidCartException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class Cart {

    private final UUID id;
    private final UUID customerId;
    private final CartStatus status;
    private final List<CartItem> items;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Cart(
            UUID id,
            UUID customerId,
            CartStatus status,
            List<CartItem> items,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.customerId = customerId;
        this.status = status;
        this.items = items;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Cart create(
            UUID id,
            UUID customerId,
            CartStatus status,
            List<CartItem> items,
            Instant createdAt,
            Instant updatedAt) {
        return of(id, customerId, status, items, createdAt, updatedAt);
    }

    public static Cart reconstitute(
            UUID id,
            UUID customerId,
            CartStatus status,
            List<CartItem> items,
            Instant createdAt,
            Instant updatedAt) {
        return of(id, customerId, status, items, createdAt, updatedAt);
    }

    public Cart addProduct(
            UUID itemId, UUID productId, int quantity, Money priceAtAddition, Instant currentTime) {
        Instant at = requireCurrentTime(currentTime);
        int existingIndex = indexOfProduct(productId);
        List<CartItem> next = new ArrayList<>(items);
        if (existingIndex < 0) {
            next.add(CartItem.create(itemId, productId, quantity, priceAtAddition, at, at));
        } else {
            CartItem existing = next.get(existingIndex);
            next.set(existingIndex, existing.changeQuantity(existing.quantity() + quantity, at));
        }
        return of(id, customerId, status, next, createdAt, at);
    }

    public Cart changeQuantity(UUID productId, int quantity, Instant currentTime) {
        Instant at = requireCurrentTime(currentTime);
        int existingIndex = requireProductIndex(productId);
        List<CartItem> next = new ArrayList<>(items);
        next.set(existingIndex, next.get(existingIndex).changeQuantity(quantity, at));
        return of(id, customerId, status, next, createdAt, at);
    }

    public Cart removeProduct(UUID productId, Instant currentTime) {
        Instant at = requireCurrentTime(currentTime);
        int existingIndex = requireProductIndex(productId);
        List<CartItem> next = new ArrayList<>(items);
        next.remove(existingIndex);
        return of(id, customerId, status, next, createdAt, at);
    }

    public Cart clear(Instant currentTime) {
        Instant at = requireCurrentTime(currentTime);
        return of(id, customerId, status, List.of(), createdAt, at);
    }

    public UUID id() {
        return id;
    }

    public UUID customerId() {
        return customerId;
    }

    public CartStatus status() {
        return status;
    }

    public List<CartItem> items() {
        return items;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    private static Cart of(
            UUID id,
            UUID customerId,
            CartStatus status,
            List<CartItem> items,
            Instant createdAt,
            Instant updatedAt) {
        requireNonNull(id, "id");
        requireNonNull(customerId, "customerId");
        requireNonNull(status, "status");
        requireNonNull(createdAt, "createdAt");
        requireNonNull(updatedAt, "updatedAt");
        if (createdAt.isAfter(updatedAt)) {
            throw new InvalidCartException("createdAt must not be after updatedAt");
        }
        return new Cart(id, customerId, status, copyItems(items), createdAt, updatedAt);
    }

    private static List<CartItem> copyItems(List<CartItem> items) {
        if (items == null) {
            throw new InvalidCartException("items cannot be null");
        }
        List<CartItem> copy = new ArrayList<>();
        Set<UUID> productIds = new HashSet<>();
        for (CartItem item : items) {
            if (item == null) {
                throw new InvalidCartException("cart items cannot contain null");
            }
            if (!productIds.add(item.productId())) {
                throw new InvalidCartException("cart cannot contain duplicate products");
            }
            copy.add(item);
        }
        return List.copyOf(copy);
    }

    private int indexOfProduct(UUID productId) {
        requireNonNull(productId, "productId");
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).productId().equals(productId)) {
                return i;
            }
        }
        return -1;
    }

    private int requireProductIndex(UUID productId) {
        int index = indexOfProduct(productId);
        if (index < 0) {
            throw new InvalidCartException("product is not in the cart");
        }
        return index;
    }

    private Instant requireCurrentTime(Instant currentTime) {
        requireNonNull(currentTime, "currentTime");
        if (currentTime.isBefore(createdAt)) {
            throw new InvalidCartException("currentTime must not be before createdAt");
        }
        return currentTime;
    }

    private static void requireNonNull(Object value, String field) {
        if (value == null) {
            throw new InvalidCartException(field + " cannot be null");
        }
    }
}
