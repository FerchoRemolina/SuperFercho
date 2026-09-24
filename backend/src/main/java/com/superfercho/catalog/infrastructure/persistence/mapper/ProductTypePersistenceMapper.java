package com.superfercho.catalog.infrastructure.persistence.mapper;

import com.superfercho.catalog.domain.model.ProductType;
import com.superfercho.catalog.infrastructure.persistence.entity.ProductTypeJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class ProductTypePersistenceMapper {

    public ProductTypeJpaEntity toEntity(ProductType productType) {
        return new ProductTypeJpaEntity(
                productType.id(),
                productType.categoryId(),
                productType.name(),
                productType.description(),
                productType.status(),
                productType.createdAt(),
                productType.updatedAt());
    }

    public ProductType toDomain(ProductTypeJpaEntity entity) {
        return ProductType.create(
                entity.getId(),
                entity.getCategoryId(),
                entity.getName(),
                entity.getDescription(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
