package com.superfercho.shopping.infrastructure.rest.dto;

import com.superfercho.shopping.application.dto.shoppinglist.AddProductToShoppingListResult;

/**
 * Respuesta de añadir un producto a una lista. {@code alreadyPresent} informa
 * al cliente que el producto ya pertenecía a la lista (operación idempotente,
 * no es un error).
 */
public record AddShoppingListItemRestResponse(ShoppingListRestResponse shoppingList, boolean alreadyPresent) {

    public static AddShoppingListItemRestResponse from(AddProductToShoppingListResult result) {
        return new AddShoppingListItemRestResponse(
                ShoppingListRestResponse.from(result.shoppingList()), result.alreadyPresent());
    }
}
