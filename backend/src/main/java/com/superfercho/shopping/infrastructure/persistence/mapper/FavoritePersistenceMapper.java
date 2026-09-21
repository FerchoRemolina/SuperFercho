package com.superfercho.shopping.infrastructure.persistence.mapper;

import com.superfercho.shopping.domain.model.Favorite;
import com.superfercho.shopping.infrastructure.persistence.entity.FavoriteJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class FavoritePersistenceMapper {

    public FavoriteJpaEntity toEntity(Favorite favorite) {
        return new FavoriteJpaEntity(
                favorite.id(), favorite.customerId(), favorite.productId(), favorite.createdAt());
    }

    public Favorite toDomain(FavoriteJpaEntity entity) {
        return Favorite.create(entity.getId(), entity.getCustomerId(), entity.getProductId(), entity.getCreatedAt());
    }
}
