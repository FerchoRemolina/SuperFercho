package com.superfercho.catalog.infrastructure.persistence.mapper;

import com.superfercho.catalog.domain.model.ProductVariant;
import com.superfercho.catalog.infrastructure.persistence.entity.ProductVariantJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class ProductVariantPersistenceMapper {

    public ProductVariantJpaEntity toEntity(ProductVariant productVariant) {
        return new ProductVariantJpaEntity(
                productVariant.id(),
                productVariant.productTypeId(),
                productVariant.name(),
                productVariant.description(),
                productVariant.status(),
                productVariant.createdAt(),
                productVariant.updatedAt());
    }

    public ProductVariant toDomain(ProductVariantJpaEntity entity) {
        return ProductVariant.create(
                entity.getId(),
                entity.getProductTypeId(),
                entity.getName(),
                entity.getDescription(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
