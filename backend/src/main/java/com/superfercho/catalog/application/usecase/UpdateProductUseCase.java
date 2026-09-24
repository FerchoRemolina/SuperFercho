package com.superfercho.catalog.application.usecase;

import com.superfercho.catalog.application.dto.ProductResult;
import com.superfercho.catalog.application.dto.UpdateProductCommand;
import com.superfercho.catalog.application.exception.InvalidProductTypeReferenceException;
import com.superfercho.catalog.application.exception.InvalidProductVariantReferenceException;
import com.superfercho.catalog.application.exception.ProductNotFoundException;
import com.superfercho.catalog.application.port.ProductRepository;
import com.superfercho.catalog.application.port.ProductTypeRepository;
import com.superfercho.catalog.application.port.ProductVariantRepository;
import com.superfercho.catalog.domain.model.Product;
import com.superfercho.catalog.domain.model.ProductType;
import com.superfercho.catalog.domain.model.ProductVariant;
import java.time.Clock;
import java.util.UUID;

public final class UpdateProductUseCase {

    private final ProductRepository productRepository;
    private final ProductTypeRepository productTypeRepository;
    private final ProductVariantRepository productVariantRepository;
    private final Clock clock;

    public UpdateProductUseCase(
            ProductRepository productRepository,
            ProductTypeRepository productTypeRepository,
            ProductVariantRepository productVariantRepository,
            Clock clock) {
        this.productRepository = productRepository;
        this.productTypeRepository = productTypeRepository;
        this.productVariantRepository = productVariantRepository;
        this.clock = clock;
    }

    public ProductResult execute(UpdateProductCommand command) {
        Product product = productRepository
                .findById(command.productId())
                .orElseThrow(() -> new ProductNotFoundException(command.productId()));
        ProductType productType = productTypeRepository
                .findById(command.productTypeId())
                .orElseThrow(() -> new InvalidProductTypeReferenceException(command.productTypeId()));
        UUID productVariantId = resolveVariantId(command.productTypeId(), command.productVariantId());

        Product updated = product.updateInformation(
                productType.categoryId(),
                productType.id(),
                productVariantId,
                command.presentation(),
                command.barcode(),
                command.name(),
                command.brand(),
                command.description(),
                command.imageUrl(),
                clock.instant());
        return ProductResult.from(productRepository.save(updated));
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
