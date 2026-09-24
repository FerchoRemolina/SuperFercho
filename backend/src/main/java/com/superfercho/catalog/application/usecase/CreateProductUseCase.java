package com.superfercho.catalog.application.usecase;

import com.superfercho.catalog.application.dto.CreateProductCommand;
import com.superfercho.catalog.application.dto.ProductResult;
import com.superfercho.catalog.application.exception.InvalidProductTypeReferenceException;
import com.superfercho.catalog.application.exception.InvalidProductVariantReferenceException;
import com.superfercho.catalog.application.port.ProductRepository;
import com.superfercho.catalog.application.port.ProductTypeRepository;
import com.superfercho.catalog.application.port.ProductVariantRepository;
import com.superfercho.catalog.domain.model.Product;
import com.superfercho.catalog.domain.model.ProductStatus;
import com.superfercho.catalog.domain.model.ProductType;
import com.superfercho.catalog.domain.model.ProductVariant;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

public final class CreateProductUseCase {

    private final ProductRepository productRepository;
    private final ProductTypeRepository productTypeRepository;
    private final ProductVariantRepository productVariantRepository;
    private final Clock clock;

    public CreateProductUseCase(
            ProductRepository productRepository,
            ProductTypeRepository productTypeRepository,
            ProductVariantRepository productVariantRepository,
            Clock clock) {
        this.productRepository = productRepository;
        this.productTypeRepository = productTypeRepository;
        this.productVariantRepository = productVariantRepository;
        this.clock = clock;
    }

    public ProductResult execute(CreateProductCommand command) {
        ProductType productType = productTypeRepository
                .findById(command.productTypeId())
                .orElseThrow(() -> new InvalidProductTypeReferenceException(command.productTypeId()));
        UUID productVariantId = resolveVariantId(command.productTypeId(), command.productVariantId());

        Instant now = clock.instant();
        Product product = Product.create(
                UUID.randomUUID(),
                productType.categoryId(),
                productType.id(),
                productVariantId,
                command.presentation(),
                command.barcode(),
                command.name(),
                command.brand(),
                command.description(),
                command.price(),
                command.stock(),
                command.imageUrl(),
                ProductStatus.ACTIVE,
                now,
                now);
        return ProductResult.from(productRepository.save(product));
    }

    private UUID resolveVariantId(UUID productTypeId, UUID productVariantId) {
        if (productVariantId == null) {
            return null;
        }
        ProductVariant variant = productVariantRepository
                .findById(productVariantId)
                .orElseThrow(() -> new InvalidProductVariantReferenceException(productVariantId));
        if (!variant.productTypeId().equals(productTypeId)) {
            throw new InvalidProductVariantReferenceException(
                    "Product variant does not belong to product type: " + productVariantId);
        }
        return variant.id();
    }
}
