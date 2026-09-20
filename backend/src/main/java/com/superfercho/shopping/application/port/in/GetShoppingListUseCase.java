package com.superfercho.shopping.application.port.in;

import com.superfercho.shopping.application.dto.shoppinglist.GetShoppingListQuery;
import com.superfercho.shopping.application.dto.shoppinglist.ShoppingListResponse;

public interface GetShoppingListUseCase {

    ShoppingListResponse execute(GetShoppingListQuery query);
}
