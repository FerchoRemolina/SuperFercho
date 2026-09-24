package com.superfercho.catalog.infrastructure.persistence.mapper;

import com.superfercho.catalog.domain.model.Presentation;
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
                product.productTypeId(),
                product.productVariantId(),
                product.presentation().quantity(),
                product.presentation().unit(),
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
                entity.getProductTypeId(),
                entity.getProductVariantId(),
                new Presentation(entity.getPresentationQuantity(), entity.getPresentationUnit()),
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
