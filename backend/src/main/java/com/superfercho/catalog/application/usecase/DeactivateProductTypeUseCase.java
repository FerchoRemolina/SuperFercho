package com.superfercho.catalog.application.usecase;

import com.superfercho.catalog.application.dto.DeactivateProductTypeCommand;
import com.superfercho.catalog.application.dto.ProductTypeResult;
import com.superfercho.catalog.application.exception.ProductTypeNotFoundException;
import com.superfercho.catalog.application.port.ProductTypeRepository;
import com.superfercho.catalog.domain.model.ProductType;
import java.time.Clock;

public final class DeactivateProductTypeUseCase {

    private final ProductTypeRepository productTypeRepository;
    private final Clock clock;

    public DeactivateProductTypeUseCase(ProductTypeRepository productTypeRepository, Clock clock) {
        this.productTypeRepository = productTypeRepository;
        this.clock = clock;
    }

    public ProductTypeResult execute(DeactivateProductTypeCommand command) {
        ProductType productType = productTypeRepository
                .findById(command.productTypeId())
                .orElseThrow(() -> new ProductTypeNotFoundException(command.productTypeId()));
        return ProductTypeResult.from(productTypeRepository.save(productType.deactivate(clock.instant())));
    }
}
