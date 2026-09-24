package com.superfercho.catalog.application.usecase;

import com.superfercho.catalog.application.dto.CreateProductTypeCommand;
import com.superfercho.catalog.application.dto.ProductTypeResult;
import com.superfercho.catalog.application.exception.InvalidCategoryReferenceException;
import com.superfercho.catalog.application.port.CategoryRepository;
import com.superfercho.catalog.application.port.ProductTypeRepository;
import com.superfercho.catalog.domain.model.ProductType;
import com.superfercho.catalog.domain.model.ProductTypeStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

public final class CreateProductTypeUseCase {

    private final ProductTypeRepository productTypeRepository;
    private final CategoryRepository categoryRepository;
    private final Clock clock;

    public CreateProductTypeUseCase(
            ProductTypeRepository productTypeRepository, CategoryRepository categoryRepository, Clock clock) {
        this.productTypeRepository = productTypeRepository;
        this.categoryRepository = categoryRepository;
        this.clock = clock;
    }

    public ProductTypeResult execute(CreateProductTypeCommand command) {
        categoryRepository
                .findById(command.categoryId())
                .orElseThrow(() -> new InvalidCategoryReferenceException(command.categoryId()));
        Instant now = clock.instant();
        ProductType productType = ProductType.create(
                UUID.randomUUID(),
                command.categoryId(),
                command.name(),
                command.description(),
                ProductTypeStatus.ACTIVE,
                now,
                now);
        return ProductTypeResult.from(productTypeRepository.save(productType));
    }
}
