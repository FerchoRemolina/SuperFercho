package com.superfercho.catalog.application.usecase;

import com.superfercho.catalog.application.dto.ListProductVariantsCommand;
import com.superfercho.catalog.application.dto.ProductVariantResult;
import com.superfercho.catalog.application.port.ProductVariantRepository;
import java.util.List;

public final class ListProductVariantsUseCase {

    private final ProductVariantRepository productVariantRepository;

    public ListProductVariantsUseCase(ProductVariantRepository productVariantRepository) {
        this.productVariantRepository = productVariantRepository;
    }

    public List<ProductVariantResult> execute(ListProductVariantsCommand command) {
        return productVariantRepository.findByProductTypeId(command.productTypeId()).stream()
                .map(ProductVariantResult::from)
                .toList();
    }
}
