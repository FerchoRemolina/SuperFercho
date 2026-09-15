package com.superfercho.shopping.application.port.in;

import com.superfercho.shopping.application.dto.shoppinglist.ClearShoppingListCommand;
import com.superfercho.shopping.application.dto.shoppinglist.ShoppingListResponse;

public interface ClearShoppingListUseCase {

    ShoppingListResponse execute(ClearShoppingListCommand command);
}
