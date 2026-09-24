package com.superfercho.catalog.application.usecase;

import com.superfercho.catalog.application.dto.CreateProductVariantCommand;
import com.superfercho.catalog.application.dto.ProductVariantResult;
import com.superfercho.catalog.application.exception.InvalidProductTypeReferenceException;
import com.superfercho.catalog.application.port.ProductTypeRepository;
import com.superfercho.catalog.application.port.ProductVariantRepository;
import com.superfercho.catalog.domain.model.ProductVariant;
import com.superfercho.catalog.domain.model.ProductVariantStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

public final class CreateProductVariantUseCase {

    private final ProductVariantRepository productVariantRepository;
    private final ProductTypeRepository productTypeRepository;
    private final Clock clock;

    public CreateProductVariantUseCase(
            ProductVariantRepository productVariantRepository,
            ProductTypeRepository productTypeRepository,
            Clock clock) {
        this.productVariantRepository = productVariantRepository;
        this.productTypeRepository = productTypeRepository;
        this.clock = clock;
    }

    public ProductVariantResult execute(CreateProductVariantCommand command) {
        productTypeRepository
                .findById(command.productTypeId())
                .orElseThrow(() -> new InvalidProductTypeReferenceException(command.productTypeId()));
        Instant now = clock.instant();
        ProductVariant productVariant = ProductVariant.create(
                UUID.randomUUID(),
                command.productTypeId(),
                command.name(),
                command.description(),
                ProductVariantStatus.ACTIVE,
                now,
                now);
        return ProductVariantResult.from(productVariantRepository.save(productVariant));
    }
}
