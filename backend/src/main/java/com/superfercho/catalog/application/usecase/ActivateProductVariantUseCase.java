package com.superfercho.catalog.application.usecase;

import com.superfercho.catalog.application.dto.ActivateProductVariantCommand;
import com.superfercho.catalog.application.dto.ProductVariantResult;
import com.superfercho.catalog.application.exception.ProductVariantNotFoundException;
import com.superfercho.catalog.application.port.ProductVariantRepository;
import com.superfercho.catalog.domain.model.ProductVariant;
import java.time.Clock;

public final class ActivateProductVariantUseCase {

    private final ProductVariantRepository productVariantRepository;
    private final Clock clock;

    public ActivateProductVariantUseCase(ProductVariantRepository productVariantRepository, Clock clock) {
        this.productVariantRepository = productVariantRepository;
        this.clock = clock;
    }

    public ProductVariantResult execute(ActivateProductVariantCommand command) {
        ProductVariant productVariant = productVariantRepository
                .findById(command.productVariantId())
                .orElseThrow(() -> new ProductVariantNotFoundException(command.productVariantId()));
        return ProductVariantResult.from(
                productVariantRepository.save(productVariant.activate(clock.instant())));
    }
}
