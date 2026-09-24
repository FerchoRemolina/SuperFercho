package com.superfercho.catalog.application.usecase;

import com.superfercho.catalog.application.dto.GetProductTypeCommand;
import com.superfercho.catalog.application.dto.ProductTypeResult;
import com.superfercho.catalog.application.exception.ProductTypeNotFoundException;
import com.superfercho.catalog.application.port.ProductTypeRepository;
import com.superfercho.catalog.domain.model.ProductType;

public final class GetProductTypeUseCase {

    private final ProductTypeRepository productTypeRepository;

    public GetProductTypeUseCase(ProductTypeRepository productTypeRepository) {
        this.productTypeRepository = productTypeRepository;
    }

    public ProductTypeResult execute(GetProductTypeCommand command) {
        ProductType productType = productTypeRepository
                .findById(command.productTypeId())
                .orElseThrow(() -> new ProductTypeNotFoundException(command.productTypeId()));
        return ProductTypeResult.from(productType);
    }
}
