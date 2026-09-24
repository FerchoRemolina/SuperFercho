package com.superfercho.shopping.application.port.in;

import com.superfercho.shopping.application.dto.shoppinglist.DeleteShoppingListCommand;

public interface DeleteShoppingListUseCase {

    void execute(DeleteShoppingListCommand command);
}
