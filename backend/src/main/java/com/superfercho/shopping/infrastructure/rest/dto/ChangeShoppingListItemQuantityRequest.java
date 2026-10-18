package com.superfercho.shopping.infrastructure.rest.dto;

/**
 * Cantidad de un producto dentro de una lista. A diferencia del carrito, se
 * permite 0: el producto permanece guardado en la lista pero no se incluirá
 * en una compra de la lista.
 */
public record ChangeShoppingListItemQuantityRequest(int quantity) {

    public ChangeShoppingListItemQuantityRequest {
        if (quantity < 0) {
            throw new IllegalArgumentException("quantity must be greater than or equal to 0");
        }
    }
}
