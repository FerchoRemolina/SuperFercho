package com.superfercho.catalog.infrastructure.persistence.mapper;

import com.superfercho.catalog.domain.model.Product;
import com.superfercho.catalog.infrastructure.persistence.entity.ProductJpaEntity;
import com.superfercho.platform.money.Money;
import org.springframework.stereotype.Component;

@Component
public class ProductPersistenceMapper {

    public ProductJpaEntity toEntity(Product product) {
        return new ProductJpaEntity(
                product.id(),
                product.categoryId(),
                product.barcode(),
                product.name(),
                product.brand(),
                product.description(),
                product.price().amount(),
                product.price().currency(),
                product.stock(),
                product.imageUrl(),
                product.status(),
                product.createdAt(),
                product.updatedAt());
    }

    public Product toDomain(ProductJpaEntity entity) {
        return Product.create(
                entity.getId(),
                entity.getCategoryId(),
                entity.getBarcode(),
                entity.getName(),
                entity.getBrand(),
                entity.getDescription(),
                new Money(entity.getPriceAmount(), entity.getCurrency()),
                entity.getStock(),
                entity.getImageUrl(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
