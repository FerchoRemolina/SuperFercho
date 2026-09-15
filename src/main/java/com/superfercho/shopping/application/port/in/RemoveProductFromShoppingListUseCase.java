package com.superfercho.shopping.application.port.in;

import com.superfercho.shopping.application.dto.shoppinglist.RemoveProductFromShoppingListCommand;
import com.superfercho.shopping.application.dto.shoppinglist.ShoppingListResponse;

public interface RemoveProductFromShoppingListUseCase {

    ShoppingListResponse execute(RemoveProductFromShoppingListCommand command);
}
