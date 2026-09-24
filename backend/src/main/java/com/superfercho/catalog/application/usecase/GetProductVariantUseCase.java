package com.superfercho.catalog.application.usecase;

import com.superfercho.catalog.application.dto.GetProductVariantCommand;
import com.superfercho.catalog.application.dto.ProductVariantResult;
import com.superfercho.catalog.application.exception.ProductVariantNotFoundException;
import com.superfercho.catalog.application.port.ProductVariantRepository;
import com.superfercho.catalog.domain.model.ProductVariant;

public final class GetProductVariantUseCase {

    private final ProductVariantRepository productVariantRepository;

    public GetProductVariantUseCase(ProductVariantRepository productVariantRepository) {
        this.productVariantRepository = productVariantRepository;
    }

    public ProductVariantResult execute(GetProductVariantCommand command) {
        ProductVariant productVariant = productVariantRepository
                .findById(command.productVariantId())
                .orElseThrow(() -> new ProductVariantNotFoundException(command.productVariantId()));
        return ProductVariantResult.from(productVariant);
    }
}
