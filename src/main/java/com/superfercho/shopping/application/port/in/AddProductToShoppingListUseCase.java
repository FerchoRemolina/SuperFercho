package com.superfercho.shopping.application.port.in;

import com.superfercho.shopping.application.dto.shoppinglist.AddProductToShoppingListCommand;
import com.superfercho.shopping.application.dto.shoppinglist.ShoppingListResponse;

public interface AddProductToShoppingListUseCase {

    ShoppingListResponse execute(AddProductToShoppingListCommand command);
}
