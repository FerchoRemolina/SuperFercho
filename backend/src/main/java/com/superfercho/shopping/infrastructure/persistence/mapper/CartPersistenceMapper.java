package com.superfercho.shopping.infrastructure.persistence.mapper;

import com.superfercho.platform.money.Money;
import com.superfercho.shopping.domain.model.Cart;
import com.superfercho.shopping.domain.model.CartItem;
import com.superfercho.shopping.infrastructure.persistence.entity.CartItemJpaEntity;
import com.superfercho.shopping.infrastructure.persistence.entity.CartJpaEntity;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CartPersistenceMapper {

    public CartJpaEntity toEntity(Cart cart) {
        List<CartItemJpaEntity> items = cart.items().stream().map(this::toItemEntity).toList();
        return new CartJpaEntity(
                cart.id(), cart.customerId(), cart.status(), cart.createdAt(), cart.updatedAt(), items);
    }

    public Cart toDomain(CartJpaEntity entity) {
        List<CartItem> items = entity.getItems().stream().map(this::toDomainItem).toList();
        return Cart.reconstitute(
                entity.getId(),
                entity.getCustomerId(),
                entity.getStatus(),
                items,
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    private CartItemJpaEntity toItemEntity(CartItem item) {
        return new CartItemJpaEntity(
                item.id(),
                item.productId(),
                item.quantity(),
                item.priceAtAddition().amount(),
                item.priceAtAddition().currency(),
                item.addedAt(),
                item.updatedAt());
    }

    private CartItem toDomainItem(CartItemJpaEntity entity) {
        return CartItem.create(
                entity.getId(),
                entity.getProductId(),
                entity.getQuantity(),
                new Money(entity.getPriceAtAdditionAmount(), entity.getPriceAtAdditionCurrency()),
                entity.getAddedAt(),
                entity.getUpdatedAt());
    }
}
