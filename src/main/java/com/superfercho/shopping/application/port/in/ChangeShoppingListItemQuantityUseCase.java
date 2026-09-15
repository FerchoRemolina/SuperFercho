package com.superfercho.shopping.application.port.in;

import com.superfercho.shopping.application.dto.shoppinglist.ChangeShoppingListItemQuantityCommand;
import com.superfercho.shopping.application.dto.shoppinglist.ShoppingListResponse;

public interface ChangeShoppingListItemQuantityUseCase {

    ShoppingListResponse execute(ChangeShoppingListItemQuantityCommand command);
}
