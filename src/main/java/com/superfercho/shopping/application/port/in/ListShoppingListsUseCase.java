package com.superfercho.shopping.application.port.in;

import com.superfercho.shopping.application.dto.shoppinglist.ListShoppingListsQuery;
import com.superfercho.shopping.application.dto.shoppinglist.ShoppingListResponse;
import java.util.List;

public interface ListShoppingListsUseCase {

    List<ShoppingListResponse> execute(ListShoppingListsQuery query);
}
