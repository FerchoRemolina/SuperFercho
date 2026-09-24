package com.superfercho.catalog.application.usecase;

import com.superfercho.catalog.application.dto.ListProductTypesCommand;
import com.superfercho.catalog.application.dto.ProductTypeResult;
import com.superfercho.catalog.application.port.ProductTypeRepository;
import java.util.List;

public final class ListProductTypesUseCase {

    private final ProductTypeRepository productTypeRepository;

    public ListProductTypesUseCase(ProductTypeRepository productTypeRepository) {
        this.productTypeRepository = productTypeRepository;
    }

    public List<ProductTypeResult> execute(ListProductTypesCommand command) {
        return productTypeRepository.findByCategoryId(command.categoryId()).stream()
                .map(ProductTypeResult::from)
                .toList();
    }
}
