package com.superfercho.shopping.domain.model;

import com.superfercho.shopping.domain.exception.InvalidShoppingListException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class ShoppingList {

    private final UUID id;
    private final UUID customerId;
    private final String name;
    private final List<ShoppingListItem> items;
    private final Instant createdAt;
    private final Instant updatedAt;

    private ShoppingList(
            UUID id,
            UUID customerId,
            String name,
            List<ShoppingListItem> items,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.customerId = customerId;
        this.name = name;
        this.items = items;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ShoppingList create(
            UUID id,
            UUID customerId,
            String name,
            List<ShoppingListItem> items,
            Instant createdAt,
            Instant updatedAt) {
        return of(id, customerId, name, items, createdAt, updatedAt);
    }

    public static ShoppingList reconstitute(
            UUID id,
            UUID customerId,
            String name,
            List<ShoppingListItem> items,
            Instant createdAt,
            Instant updatedAt) {
        return of(id, customerId, name, items, createdAt, updatedAt);
    }

    public ShoppingList changeName(String name, Instant currentTime) {
        Instant at = requireCurrentTime(currentTime);
        return of(id, customerId, name, items, createdAt, at);
    }

    public ShoppingList addProduct(UUID itemId, UUID productId, int quantity, Instant currentTime) {
        Instant at = requireCurrentTime(currentTime);
        int existingIndex = indexOfProduct(productId);
        List<ShoppingListItem> next = new ArrayList<>(items);
        if (existingIndex < 0) {
            next.add(ShoppingListItem.create(itemId, productId, quantity, at));
        } else {
            ShoppingListItem existing = next.get(existingIndex);
            next.set(existingIndex, existing.changeQuantity(existing.quantity() + quantity));
        }
        return of(id, customerId, name, next, createdAt, at);
    }

    public ShoppingList changeQuantity(UUID productId, int quantity, Instant currentTime) {
        Instant at = requireCurrentTime(currentTime);
        int existingIndex = requireProductIndex(productId);
        List<ShoppingListItem> next = new ArrayList<>(items);
        next.set(existingIndex, next.get(existingIndex).changeQuantity(quantity));
        return of(id, customerId, name, next, createdAt, at);
    }

    public ShoppingList removeProduct(UUID productId, Instant currentTime) {
        Instant at = requireCurrentTime(currentTime);
        int existingIndex = requireProductIndex(productId);
        List<ShoppingListItem> next = new ArrayList<>(items);
        next.remove(existingIndex);
        return of(id, customerId, name, next, createdAt, at);
    }

    public ShoppingList clear(Instant currentTime) {
        Instant at = requireCurrentTime(currentTime);
        return of(id, customerId, name, List.of(), createdAt, at);
    }

    public UUID id() {
        return id;
    }

    public UUID customerId() {
        return customerId;
    }

    public String name() {
        return name;
    }

    public List<ShoppingListItem> items() {
        return items;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    private static ShoppingList of(
            UUID id,
            UUID customerId,
            String name,
            List<ShoppingListItem> items,
            Instant createdAt,
            Instant updatedAt) {
        requireNonNull(id, "id");
        requireNonNull(customerId, "customerId");
        requireText(name, "name");
        requireNonNull(createdAt, "createdAt");
        requireNonNull(updatedAt, "updatedAt");
        if (createdAt.isAfter(updatedAt)) {
            throw new InvalidShoppingListException("createdAt must not be after updatedAt");
        }
        return new ShoppingList(id, customerId, name, copyItems(items), createdAt, updatedAt);
    }

    private static List<ShoppingListItem> copyItems(List<ShoppingListItem> items) {
        if (items == null) {
            throw new InvalidShoppingListException("items cannot be null");
        }
        List<ShoppingListItem> copy = new ArrayList<>();
        Set<UUID> productIds = new HashSet<>();
        for (ShoppingListItem item : items) {
            if (item == null) {
                throw new InvalidShoppingListException("shopping list items cannot contain null");
            }
            if (!productIds.add(item.productId())) {
                throw new InvalidShoppingListException("shopping list cannot contain duplicate products");
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
            throw new InvalidShoppingListException("product is not in the shopping list");
        }
        return index;
    }

    private Instant requireCurrentTime(Instant currentTime) {
        requireNonNull(currentTime, "currentTime");
        if (currentTime.isBefore(createdAt)) {
            throw new InvalidShoppingListException("currentTime must not be before createdAt");
        }
        return currentTime;
    }

    private static void requireNonNull(Object value, String field) {
        if (value == null) {
            throw new InvalidShoppingListException(field + " cannot be null");
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidShoppingListException(field + " cannot be null or blank");
        }
    }
}
