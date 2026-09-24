package com.superfercho.catalog.application.usecase;

import com.superfercho.catalog.application.dto.ProductTypeResult;
import com.superfercho.catalog.application.dto.UpdateProductTypeCommand;
import com.superfercho.catalog.application.exception.ProductTypeNotFoundException;
import com.superfercho.catalog.application.port.ProductTypeRepository;
import com.superfercho.catalog.domain.model.ProductType;
import java.time.Clock;

public final class UpdateProductTypeUseCase {

    private final ProductTypeRepository productTypeRepository;
    private final Clock clock;

    public UpdateProductTypeUseCase(ProductTypeRepository productTypeRepository, Clock clock) {
        this.productTypeRepository = productTypeRepository;
        this.clock = clock;
    }

    public ProductTypeResult execute(UpdateProductTypeCommand command) {
        ProductType productType = productTypeRepository
                .findById(command.productTypeId())
                .orElseThrow(() -> new ProductTypeNotFoundException(command.productTypeId()));
        ProductType updated =
                productType.updateInformation(command.name(), command.description(), clock.instant());
        return ProductTypeResult.from(productTypeRepository.save(updated));
    }
}
