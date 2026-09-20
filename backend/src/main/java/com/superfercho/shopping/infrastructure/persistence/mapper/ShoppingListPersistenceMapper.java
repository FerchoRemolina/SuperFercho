package com.superfercho.shopping.infrastructure.persistence.mapper;

import com.superfercho.shopping.domain.model.ShoppingList;
import com.superfercho.shopping.domain.model.ShoppingListItem;
import com.superfercho.shopping.infrastructure.persistence.entity.ShoppingListItemJpaEntity;
import com.superfercho.shopping.infrastructure.persistence.entity.ShoppingListJpaEntity;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ShoppingListPersistenceMapper {

    public ShoppingListJpaEntity toEntity(ShoppingList shoppingList) {
        List<ShoppingListItemJpaEntity> items =
                shoppingList.items().stream().map(this::toItemEntity).toList();
        return new ShoppingListJpaEntity(
                shoppingList.id(),
                shoppingList.customerId(),
                shoppingList.name(),
                shoppingList.createdAt(),
                shoppingList.updatedAt(),
                items);
    }

    public ShoppingList toDomain(ShoppingListJpaEntity entity) {
        List<ShoppingListItem> items = entity.getItems().stream().map(this::toDomainItem).toList();
        return ShoppingList.reconstitute(
                entity.getId(),
                entity.getCustomerId(),
                entity.getName(),
                items,
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    private ShoppingListItemJpaEntity toItemEntity(ShoppingListItem item) {
        return new ShoppingListItemJpaEntity(item.id(), item.productId(), item.quantity(), item.createdAt());
    }

    private ShoppingListItem toDomainItem(ShoppingListItemJpaEntity entity) {
        return ShoppingListItem.create(
                entity.getId(), entity.getProductId(), entity.getQuantity(), entity.getCreatedAt());
    }
}
