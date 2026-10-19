package com.superfercho.shopping.application.port.in;

import com.superfercho.shopping.application.dto.shoppinglist.AddProductToShoppingListCommand;
import com.superfercho.shopping.application.dto.shoppinglist.AddProductToShoppingListResult;

public interface AddProductToShoppingListUseCase {

    AddProductToShoppingListResult execute(AddProductToShoppingListCommand command);
}
