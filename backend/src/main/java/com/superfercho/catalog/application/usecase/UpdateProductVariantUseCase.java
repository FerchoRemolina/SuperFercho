package com.superfercho.catalog.application.usecase;

import com.superfercho.catalog.application.dto.ProductVariantResult;
import com.superfercho.catalog.application.dto.UpdateProductVariantCommand;
import com.superfercho.catalog.application.exception.ProductVariantNotFoundException;
import com.superfercho.catalog.application.port.ProductVariantRepository;
import com.superfercho.catalog.domain.model.ProductVariant;
import java.time.Clock;

public final class UpdateProductVariantUseCase {

    private final ProductVariantRepository productVariantRepository;
    private final Clock clock;

    public UpdateProductVariantUseCase(ProductVariantRepository productVariantRepository, Clock clock) {
        this.productVariantRepository = productVariantRepository;
        this.clock = clock;
    }

    public ProductVariantResult execute(UpdateProductVariantCommand command) {
        ProductVariant productVariant = productVariantRepository
                .findById(command.productVariantId())
                .orElseThrow(() -> new ProductVariantNotFoundException(command.productVariantId()));
        ProductVariant updated =
                productVariant.updateInformation(command.name(), command.description(), clock.instant());
        return ProductVariantResult.from(productVariantRepository.save(updated));
    }
}
