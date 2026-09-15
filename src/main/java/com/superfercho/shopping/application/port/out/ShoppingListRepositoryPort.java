package com.superfercho.shopping.application.port.out;

import com.superfercho.shopping.domain.model.ShoppingList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShoppingListRepositoryPort {

    ShoppingList save(ShoppingList shoppingList);

    Optional<ShoppingList> findById(UUID shoppingListId);

    List<ShoppingList> findAllByCustomerId(UUID customerId);
}
