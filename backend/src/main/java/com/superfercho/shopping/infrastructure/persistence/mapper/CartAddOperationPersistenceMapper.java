package com.superfercho.shopping.infrastructure.persistence.mapper;

import com.superfercho.shopping.application.dto.cart.CartAddOperation;
import com.superfercho.shopping.infrastructure.persistence.entity.CartAddOperationJpaEntity;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CartAddOperationPersistenceMapper {

    public CartAddOperationJpaEntity toEntity(CartAddOperation operation) {
        return new CartAddOperationJpaEntity(
                UUID.randomUUID(),
                operation.operationId(),
                operation.customerId(),
                operation.productId(),
                operation.quantity(),
                operation.createdAt());
    }

    public CartAddOperation toDomain(CartAddOperationJpaEntity entity) {
        return new CartAddOperation(
                entity.getOperationId(),
                entity.getCustomerId(),
                entity.getProductId(),
                entity.getQuantity(),
                entity.getCreatedAt());
    }
}
