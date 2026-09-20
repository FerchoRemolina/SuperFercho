package com.superfercho.shopping.application.port.in;

import com.superfercho.shopping.application.dto.shoppinglist.CreateShoppingListCommand;
import com.superfercho.shopping.application.dto.shoppinglist.ShoppingListResponse;

public interface CreateShoppingListUseCase {

    ShoppingListResponse execute(CreateShoppingListCommand command);
}
