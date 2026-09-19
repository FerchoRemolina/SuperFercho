package com.superfercho.shopping.application.port.in;

import com.superfercho.shopping.application.dto.cart.CartResponse;
import com.superfercho.shopping.application.dto.shoppinglist.AddShoppingListToCartCommand;

public interface AddShoppingListToCartUseCase {

    CartResponse execute(AddShoppingListToCartCommand command);
}
