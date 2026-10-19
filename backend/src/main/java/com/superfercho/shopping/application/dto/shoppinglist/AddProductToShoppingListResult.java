package com.superfercho.shopping.application.dto.shoppinglist;

import com.superfercho.shopping.domain.model.ShoppingList;

/**
 * Resultado de añadir un producto a una lista. La operación es idempotente:
 * {@code alreadyPresent} distingue "agregado ahora" de "ya pertenecía a la
 * lista" sin que esto último sea un error.
 */
public record AddProductToShoppingListResult(ShoppingListResponse shoppingList, boolean alreadyPresent) {

    public static AddProductToShoppingListResult added(ShoppingList shoppingList) {
        return new AddProductToShoppingListResult(ShoppingListResponse.from(shoppingList), false);
    }

    public static AddProductToShoppingListResult alreadyPresent(ShoppingList shoppingList) {
        return new AddProductToShoppingListResult(ShoppingListResponse.from(shoppingList), true);
    }
}
