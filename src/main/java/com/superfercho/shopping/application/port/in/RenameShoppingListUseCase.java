package com.superfercho.shopping.application.port.in;

import com.superfercho.shopping.application.dto.shoppinglist.RenameShoppingListCommand;
import com.superfercho.shopping.application.dto.shoppinglist.ShoppingListResponse;

public interface RenameShoppingListUseCase {

    ShoppingListResponse execute(RenameShoppingListCommand command);
}
